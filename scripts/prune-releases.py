#!/usr/bin/env python3
"""Retain the verified current desktop download; never prune before successful publication."""
import argparse
import json
import re
import subprocess


def deletion_plan(releases, keep, commit):
    matches = [r for r in releases if r['tag_name'] == keep]
    if len(matches) != 1:
        raise ValueError('Expected exactly one retained release')
    current = matches[0]
    if current.get('draft') or current['target_commitish'] != commit:
        raise ValueError('Retained release is not the expected published commit')
    match = re.fullmatch(r'v(\d+\.\d+\.\d+)-desktop\.\d+', keep)
    if not match or not any(a['name'] in ('AlphaDesktop-' + match[1] + '.exe', 'MinecraftAlphaLauncher.exe') and a['size'] > 1000000 for a in current['assets']):
        raise ValueError('Verified replacement Windows installer missing')
    return [r['tag_name'] for r in releases
            if r['tag_name'] != keep and not r.get('draft')
            and re.fullmatch(r'v\d+\.\d+\.\d+-desktop\.\d+', r['tag_name'])
            and r['created_at'] <= current['created_at']]


def main():
    parser = argparse.ArgumentParser()
    for name in ('repo', 'keep', 'expected-commit'):
        parser.add_argument('--' + name, required=True)
    args = parser.parse_args()
    output = subprocess.check_output(['gh', 'api', '--paginate', 'repos/' + args.repo + '/releases', '--jq', '.[]'], text=True)
    releases = [json.loads(line) for line in output.splitlines() if line.strip()]
    tags = deletion_plan(releases, args.keep, args.expected_commit)
    for tag in tags:
        subprocess.run(['gh', 'release', 'delete', tag, '--repo', args.repo, '--cleanup-tag', '--yes'], check=True)
    print('Retained', args.keep, '; removed', len(tags), 'older desktop releases and their tags.')


if __name__ == '__main__':
    main()
