#!/bin/sh -x
mvn package
mvn source:jar
mvn javadoc:javadoc
gpg -ab pom.xml
cp pom.xml target
mv pom.xml.asc target
list_of_files="pom.xml pom.xml.asc"
cd target
for i in *.jar; do
	gpg -ab $i
	list_of_files="$list_of_files $i"
done
for i in *.jar.asc; do
	list_of_files="$list_of_files $i"
done
rm bundle.jar.asc
jar -cvf bundle.jar $list_of_files
cd ..
