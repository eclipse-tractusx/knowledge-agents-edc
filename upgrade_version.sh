echo Upgrading from 1.10.6-SNAPSHOT to $1
PATTERN=s/1.10.6-SNAPSHOT/$1/g
LC_ALL=C
find ./ -type f -regex *.(xml|sh|yml|yaml|md|java|properties) -exec sed -i.bak $PATTERN {} \;