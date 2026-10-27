import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from uuid import uuid4

spec = importlib.util.spec_from_file_location("prepare", Path(__file__).with_name("prepare-catalog-load.py"))
prepare = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare)
TEST_TEMP = Path(__file__).resolve().parents[1] / "target"
TEST_TEMP.mkdir(exist_ok=True)


class PrepareCatalogTests(unittest.TestCase):
    def fixture(self, directory, count=1):
        source = directory / "source"
        source.mkdir()
        payload = {"audience": "Colaboradores", "participationInstructions": "  Aapresentar crachá.\n",
                   "contact": "Contato", "discounts": [{"value": "20%", "description": "20%"}]}
        offers = [{"reference": f"PDF-{i:03d}", "partnerName": "Parceiro", "category": "Categoria",
                   "sourcePages": [1], "payload": payload, "deliveryMode": "ONLINE",
                   "literalSource": {"name": "Parceiro", "discountText": "20%"}} for i in range(count)]
        prepare.write(source, "00_benefits_manifest.json", {"detailedBenefits": offers})
        prepare.write(source, "01_categories_batch.json", {"categories": []})
        prepare.write(source, "03_establishments_research_not_api_payload.json", {"units": []})
        return source

    def test_batches_preserve_literal_text_and_do_not_invent_ids(self):
        with tempfile.TemporaryDirectory(dir=TEST_TEMP) as temporary:
            root = Path(temporary)
            source = self.fixture(root, 112)
            output = root / "output"
            summary = prepare.prepare(source, output)
            self.assertEqual(112, summary["benefits"])
            self.assertEqual(0, summary["readyEstablishments"])
            self.assertEqual(100, len(prepare.read(output / "02_benefits_batch_001.json")["benefits"]))
            tail = prepare.read(output / "02_benefits_batch_002.json")["benefits"]
            self.assertEqual(12, len(tail))
            self.assertEqual("  Aapresentar crachá.\n", tail[0]["participationInstructions"])
            self.assertFalse(list(output.glob("06*")))
            with self.assertRaises(ValueError):
                prepare.prepare(source, output)

    def test_real_ids_allow_online_payload_but_incomplete_physical_unit_stays_pending(self):
        with tempfile.TemporaryDirectory(dir=TEST_TEMP) as temporary:
            root = Path(temporary)
            source = self.fixture(root)
            prepare.write(source, "03_establishments_research_not_api_payload.json", {"units": [{
                "reference": "UNIT-001", "name": "Unidade", "relatedBenefitRefs": ["PDF-000"],
                "address": {}, "coordinatesApproved": True, "addressStatus": "primary_address"}]})
            category, benefit = str(uuid4()), str(uuid4())
            prepare.write(root, "ids.json", {"categories": {"Categoria": category}, "benefits": {"PDF-000": benefit}})
            output = root / "output"
            summary = prepare.prepare(source, output, root / "ids.json")
            self.assertEqual(1, summary["readyEstablishments"])
            self.assertEqual(1, summary["pendingEstablishments"])
            partner = prepare.read(output / "06_establishments_batch_001.json")["establishments"][0]
            self.assertEqual("ONLINE", partner["channel"])
            self.assertNotIn("latitude", partner)
            self.assertEqual(benefit, partner["benefitId"])
            self.assertEqual(category, partner["categoryId"])

    def test_conflicting_campaigns_never_pick_a_benefit_implicitly(self):
        with tempfile.TemporaryDirectory(dir=TEST_TEMP) as temporary:
            root = Path(temporary)
            source = self.fixture(root, 2)
            prepare.write(source, "03_establishments_research_not_api_payload.json", {"units": [{
                "reference": "UNIT-001", "name": "Polo", "relatedBenefitRefs": ["PDF-000", "PDF-001"],
                "address": {}, "coordinatesApproved": True, "addressStatus": "primary_address_campaign_conflict"}]})
            output = root / "output"
            prepare.prepare(source, output)
            draft = prepare.read(output / "05_establishments_review_not_api_payload.json")["establishments"][-1]
            self.assertIsNone(draft["payload"]["benefitId"])
            self.assertFalse(draft["isApiPayload"])
            self.assertTrue(any("único benefício" in item for item in draft["pending"]))


if __name__ == "__main__":
    unittest.main()
