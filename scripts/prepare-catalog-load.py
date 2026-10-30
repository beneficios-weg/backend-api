"""Prepare reviewed local catalog files; never contact the API or import data."""
import argparse
import json
import re
from pathlib import Path
from uuid import UUID

# Approved correction of category metadata; historical source files stay intact.
CATEGORY_NAMES = {"Lazer e Turismo": "Lazer"}


def read(path):
    return json.loads(Path(path).read_text(encoding="utf-8-sig"))


def write(directory, name, value):
    (directory / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def required(value):
    return isinstance(value, str) and bool(value.strip())


def check_benefit(payload):
    for key in ("audience", "participationInstructions", "contact"):
        if not required(payload.get(key)):
            raise ValueError(f"Missing nonblank benefit field: {key}")
    if not payload.get("discounts"):
        raise ValueError("At least one discount is required")
    for discount in payload["discounts"]:
        if not required(discount.get("value")) or not 2 <= len(discount["value"].strip()) <= 15:
            raise ValueError("Discount value must contain 2–15 characters")
        if not required(discount.get("description")):
            raise ValueError("Discount description must not be blank")


def real_id(mapping, family, key):
    value = mapping.get(family, {}).get(key)
    return str(UUID(value)) if value else None


def prepare(source, destination, ids=None):
    source, destination = Path(source), Path(destination)
    if destination.exists():
        raise ValueError("Destination already exists; use a new directory to preserve previous files")
    manifest = read(source / "00_benefits_manifest.json")
    offers = manifest["detailedBenefits"]
    units = read(source / "03_establishments_research_not_api_payload.json")["units"]
    mapping = read(ids) if ids else {}
    mapped_categories = {}
    for name, value in mapping.get("categories", {}).items():
        canonical = CATEGORY_NAMES.get(name, name)
        previous = mapped_categories.get(canonical)
        if previous and value and str(UUID(previous)) != str(UUID(value)):
            raise ValueError("Conflicting category IDs after approved name correction")
        mapped_categories[canonical] = value or previous
    mapping["categories"] = mapped_categories
    for offer in offers:
        offer["category"] = CATEGORY_NAMES.get(offer["category"], offer["category"])
    category_batch = read(source / "01_categories_batch.json")
    for category in category_batch["categories"]:
        category["name"] = CATEGORY_NAMES.get(category["name"], category["name"])
    names = [category["name"] for category in category_batch["categories"]]
    if len(names) != len(set(names)):
        raise ValueError("Duplicate categories after approved name correction; review existing records")
    refs = {offer["reference"]: offer for offer in offers}
    if len(refs) != len(offers):
        raise ValueError("Duplicate benefit references")
    for offer in offers:
        check_benefit(offer["payload"])
    # Validate identifiers before writing any outputs; identifiers are never generated here.
    for offer in offers:
        real_id(mapping, "categories", offer["category"])
        real_id(mapping, "benefits", offer["reference"])
    destination.mkdir(parents=True)
    write(destination, "01_categories_batch.json", category_batch)
    index = []
    for start in range(0, len(offers), 100):
        filename = f"02_benefits_batch_{start // 100 + 1:03d}.json"
        batch = offers[start:start + 100]
        write(destination, filename, {"benefits": [offer["payload"] for offer in batch]})
        for position, offer in enumerate(batch):
            index.append({"reference": offer["reference"], "partnerName": offer["partnerName"],
                          "category": offer["category"], "sourcePages": offer["sourcePages"],
                          "batchFile": filename, "responseIndex": position})
    write(destination, "03_benefit_response_index.json", index)
    write(destination, "04_ids_template.json", {
        "categories": {name: None for name in sorted({offer["category"] for offer in offers})},
        "benefits": {offer["reference"]: None for offer in offers}})

    drafts = []
    for offer in offers:
        if offer["deliveryMode"] == "ONLINE":
            drafts.append({"reference": "ONLINE-" + offer["reference"], "benefitReference": offer["reference"],
                           "categoryName": offer["category"], "pending": [], "payload": {
                               "name": offer["literalSource"]["name"],
                               "description": offer["literalSource"]["discountText"],
                               "channel": "ONLINE", "serviceIds": []}})
    allowed_statuses = {"primary_address", "user_pdf_address", "primary_address_user_reviewed"}
    for unit in units:
        references = unit["relatedBenefitRefs"]
        pending = []
        if len(references) != 1 or references[0] not in refs:
            pending.append("Escolher um único benefício existente para esta unidade; não duplicar por inferência")
            offer = None
        else:
            offer = refs[references[0]]
        if unit.get("addressStatus") not in allowed_statuses:
            pending.append("Revisar endereço/participação: " + str(unit.get("addressStatus")))
        if not unit.get("coordinatesApproved"):
            pending.append("Coordenadas não aprovadas")
        address = unit.get("address") or {}
        payload = {key: address.get(key) for key in
                   ("postalCode", "streetNumber", "street", "district", "city", "state")}
        payload.update({"name": unit["name"], "description": offer["literalSource"]["discountText"] if offer else None,
                        "channel": "HYBRID" if offer and offer["deliveryMode"] == "HYBRID" else "PHYSICAL",
                        "latitude": unit.get("latitude"), "longitude": unit.get("longitude"), "serviceIds": []})
        for field in ("postalCode", "streetNumber", "street", "district", "city", "state", "latitude", "longitude"):
            if payload[field] is None or payload[field] == "":
                pending.append("Campo ausente: " + field)
        if payload["postalCode"] is not None and not re.fullmatch(r"[0-9]{8}", str(payload["postalCode"])):
            pending.append("CEP deve conter oito dígitos")
        for field in ("street", "district", "city"):
            if payload[field] is not None and not 3 <= len(payload[field]) <= 75:
                pending.append("Comprimento inválido: " + field)
        if payload["state"] is not None and not re.fullmatch(r"[A-Za-z]{2}", payload["state"]):
            pending.append("UF inválida")
        for field, limit in (("latitude", 90), ("longitude", 180)):
            if payload[field] is not None and not -limit <= payload[field] <= limit:
                pending.append("Coordenada inválida: " + field)
        number = payload["streetNumber"]
        if number is not None and (type(number) is not int or number < 0):
            pending.append("Número predial deve ser inteiro não negativo")
        drafts.append({"reference": unit["reference"], "benefitReference": offer["reference"] if offer else None,
                       "categoryName": offer["category"] if offer else None, "pending": pending,
                       "sources": unit.get("sources", []), "notes": unit.get("notes"),
                       "addressComplement": address.get("complement"), "payload": payload})
    ready = []
    for draft in drafts:
        payload = draft["payload"]
        payload["categoryId"] = real_id(mapping, "categories", draft["categoryName"])
        payload["benefitId"] = real_id(mapping, "benefits", draft["benefitReference"])
        for field in ("categoryId", "benefitId"):
            if not payload[field]:
                draft["pending"].append("Mapear UUID real: " + field)
        draft["isApiPayload"] = not draft["pending"]
        if draft["isApiPayload"]:
            ready.append(draft)
    write(destination, "05_establishments_review_not_api_payload.json", {"establishments": drafts})
    ready_index = []
    for start in range(0, len(ready), 100):
        filename = f"06_establishments_batch_{start // 100 + 1:03d}.json"
        batch = ready[start:start + 100]
        write(destination, filename, {"establishments": [draft["payload"] for draft in batch]})
        ready_index.extend({"reference": draft["reference"], "batchFile": filename, "responseIndex": position}
                           for position, draft in enumerate(batch))
    write(destination, "07_establishment_response_index.json", ready_index)
    covered = {draft["benefitReference"] for draft in ready}
    missing = [{"reference": offer["reference"], "partnerName": offer["partnerName"],
                "category": offer["category"], "sourcePages": offer["sourcePages"],
                "pendingUnits": [{"reference": draft["reference"], "pending": draft["pending"]}
                                 for draft in drafts if draft["benefitReference"] == offer["reference"]]}
               for offer in offers if offer["reference"] not in covered]
    write(destination, "09_catalog_coverage.json", {
        "scope": "Prepared payloads only; not imported or live API data", "imported": False,
        "benefitsWithReadyEstablishment": len(covered), "benefitsWithoutReadyEstablishment": missing})
    summary = {"benefits": len(offers), "onlineDrafts": sum(o["deliveryMode"] == "ONLINE" for o in offers),
               "physicalUnitDrafts": len(units), "readyEstablishments": len(ready),
               "pendingEstablishments": len(drafts) - len(ready),
               "benefitsWithoutReadyEstablishment": len(missing), "imported": False}
    write(destination, "08_validation.json", summary)
    (destination / "README.md").write_text(
        "# Carga preparada — não importada\n\n"
        "Requer a API com V7/V8. Enviar somente os wrappers 01/02/06 aos endpoints /batch respectivos, com sessão ADMIN.\n"
        "Benefícios: lotes separados, até 100; guardar cada resposta e usar 03 para mapear índices aos PDF-IDs.\n"
        "Preencher uma cópia de 04 com UUIDs retornados pela API (categorias existentes podem ser reutilizadas).\n"
        "Rodar novamente com --ids e outra saída para produzir 06 somente com parceiros completos.\n"
        "05 é revisão, nunca payload HTTP: mostra endereços pendentes, campanhas conflitantes e UUIDs ausentes.\n"
        "09 mostra cada benefício ainda sem estabelecimento pronto; benefícios cadastrados sem vínculo não aparecem no Mobile.\n"
        "A cobertura descreve arquivos preparados, não confirma importação. Não reenviar lotes existentes.\n"
        "Nome aprovado: Lazer (requer V9). Reutilizar o UUID antigo e corrigir por PATCH, sem criar outra categoria.\n"
        "Coordenadas aprovadas não significam importação nem confirmação comercial de todas as unidades.\n"
        "Textos são preservados; cupons, datas e erros da fonte não foram corrigidos.\n"
        "ONLINE: lista, sem endereço/GPS; Mobile usa forMap=true para pins.\n"
        "Cada estabelecimento tem um único benefício; Uningá requer escolher campanha.\n"
        "Não repetir POST após timeout sem consultar a API: lotes não têm chave de idempotência.\n"
        "Não enviar rascunhos, metadados, IDs fictícios ou arquivos com senhas. Nenhuma chamada de rede é feita pelo preparador.\n",
        encoding="utf-8")
    return summary


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--ids", type=Path)
    args = parser.parse_args()
    print(json.dumps(prepare(args.source, args.output, args.ids), ensure_ascii=False))
