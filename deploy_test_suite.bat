echo Deploying test suite...
rm -f test-suite.zip
7z a test-suite.zip .\test-suite\*
curl -F updateSpecification=true -F specification=26D79B27X6053X457DX8FD7X8C8B4EAEA6D7 -F testSuite=@test-suite.zip --header "ITB_API_KEY: EC89D73EX157AX469EX8E47X00066BF6508B" -X POST http://localhost:9001/api/rest/testsuite/deploy