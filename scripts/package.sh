#!/bin/sh
# 배포 묶음 생성 (인터넷이 되는 PC에서 실행). JDK 1.8 과 Maven 이 필요하다.
# 결과: dist/arsxml2wv/ 폴더와 dist/arsxml2wv.zip
#   arsxml2wv.exe  - 옆의 jar 를 설치된 Java 로 실행하는 래퍼 (한 번 만들면 고정)
#   arsxml2wv.jar  - 프로그램 본체 (폐쇄망에서는 mvn package 로 이 파일만 다시 만들어 교체)
#   arsxml2wv.bat  - exe 를 쓸 수 없을 때의 대체 실행 파일
set -eu
cd "$(dirname "$0")/.."
mvn -B -q -P exe clean package
out=dist/arsxml2wv
rm -rf "$out" dist/arsxml2wv.zip
mkdir -p "$out"
cp target/arsxml2wv.exe target/arsxml2wv.jar packaging/arsxml2wv.bat docs/USAGE.md labels.example.properties "$out/"
(cd dist && zip -q -r arsxml2wv.zip arsxml2wv)
ls -l "$out" dist/arsxml2wv.zip
