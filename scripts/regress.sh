#!/bin/sh
# 로컬 samples/*.xml 회귀 확인. samples/ 는 Git에 올리지 않는다.
# 사용법: scripts/regress.sh          # samples/expected/<이름>/ 과 비교
#         scripts/regress.sh update   # 현재 결과를 기대값으로 저장 (결과 검토 후 실행)
# samples/<이름>.properties 가 있으면 문구 보완 파일로 함께 사용한다.
# 필요: JDK 1.8 이상, Maven (java 는 JAVA_HOME 또는 PATH 에서 찾는다)
set -eu
cd "$(dirname "$0")/.."
mvn -B -q -DskipTests package
java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

status=0
for xml in samples/*.xml; do
	name=$(basename "$xml" .xml)
	labels=""
	[ -f "samples/$name.properties" ] && labels="samples/$name.properties"
	"$java" -jar target/arsxml2wv.jar "$xml" "$tmp/out/$name" $labels >/dev/null
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
