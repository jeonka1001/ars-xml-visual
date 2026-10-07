#!/bin/sh
# 로컬 samples/*.xml 회귀 확인. samples/ 는 Git에 올리지 않는다.
# 사용법: scripts/regress.sh          # samples/expected/<이름>/ 과 비교
#         scripts/regress.sh update   # 현재 결과를 기대값으로 저장 (결과 검토 후 실행)
# samples/<이름>.properties 가 있으면 문구 보완 파일로 함께 사용한다.
set -eu
cd "$(dirname "$0")/.."
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
go build -o "$tmp/arsxml2wv" ./cmd/arsxml2wv

status=0
for xml in samples/*.xml; do
	name=$(basename "$xml" .xml)
	labels=""
	[ -f "samples/$name.properties" ] && labels="samples/$name.properties"
	"$tmp/arsxml2wv" "$xml" "$tmp/out/$name" $labels >/dev/null
	if [ "${1:-}" = update ]; then
		rm -rf "samples/expected/$name"
		mkdir -p samples/expected
		cp -R "$tmp/out/$name" "samples/expected/$name"
		echo "updated: $name"
	elif diff -r "samples/expected/$name" "$tmp/out/$name"; then
		echo "ok: $name"
	else
		echo "DIFF: $name"
		status=1
	fi
done
exit $status
