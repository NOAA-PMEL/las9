#!/bin/sh
echo "Building admin interface..."
cd projects/admin-console
./node_modules/.bin/ng build --configuration production --base-href .
cd ../..
rm -rf src/main/webapp/admin/*
cp -R projects/admin-console/dist/admin-console/* src/main/webapp/admin/.
mv src/main/webapp/admin/index.html grails-app/views/admin/index.gsp
