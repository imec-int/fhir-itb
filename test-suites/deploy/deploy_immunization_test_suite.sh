echo zip test suite...
cd ..
zip -r immunization-suite.zip immunization-suite

curl -F updateSpecification=true -F specification=01FAE6F4XC022X4FE3XBE11X1293E8F73C23 -F testSuite=@immunization-suite.zip --header "ITB_API_KEY: F6628973X0EE7X40ECX9C36X8384C71F63D2" -X POST http://localhost:9000/api/rest/testsuite/deploy
rm -f immunization-suite.zip
