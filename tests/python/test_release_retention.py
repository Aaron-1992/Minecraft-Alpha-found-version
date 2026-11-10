import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('retention', Path(__file__).resolve().parents[2] / 'scripts/prune-releases.py')
retention = importlib.util.module_from_spec(spec)
spec.loader.exec_module(retention)


class ReleaseRetentionTests(unittest.TestCase):
    def release(self, tag='v0.14.0-desktop.28', date='2026-11-08T12:00:00Z', **overrides):
        release = dict(tag_name=tag, created_at=date, target_commitish='test-commit', draft=False,
                       assets=[dict(name='AlphaDesktop-0.14.0.exe', size=2000000)])
        release.update(overrides)
        return release

    def test_only_older_published_desktop_releases_are_deleted(self):
        current = self.release()
        old = self.release('v0.13.0-desktop.26', '2026-11-07T12:00:00Z')
        newer = self.release('v0.15.0-desktop.29', '2026-11-09T12:00:00Z')
        unrelated = self.release('other-product')
        draft = self.release('v0.12.0-desktop.25', draft=True)
        self.assertEqual([old['tag_name']], retention.deletion_plan([old, current, newer, unrelated, draft], current['tag_name'], 'test-commit'))

    def test_missing_or_wrong_replacement_cannot_delete_anything(self):
        for changes in (dict(assets=[]), dict(draft=True), dict(target_commitish='different')):
            with self.assertRaises(ValueError):
                retention.deletion_plan([self.release(**changes)], 'v0.14.0-desktop.28', 'test-commit')
        with self.assertRaises(ValueError):
            retention.deletion_plan([], 'v0.14.0-desktop.28', 'test-commit')
