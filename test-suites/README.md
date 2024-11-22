# Deploying new test cases

Follow the steps below in order to add new test cases to the FHIR-Community ITB
Sandbox image.

## 1. Create test cases

Create new test case files and make sure they are added to a Test Suite.

Test suites are defined in the `test-suites` directory.

## 2. Add the new test cases (or test-suites) to ITB

### Steps:

1. [Run the current sandbox environment](../dist/README.md)
2. Deploy the new test cases to the ITB environment.
   > [!NOTE]
   > You can find shell scripts under the `/test-suites/deploy` directory that
   > you can use or adapt to automatically deploy a test-suite to your local ITB
   > environment.

## 3. Create conformance statement

As `vendor_admin` user, add the newly created test-cases to the conformance
statements.

### Steps

1. Go to `my conformance statements` tab in the ITB UI
2. Click on `Create Statement` button in the top right corner
3. Select the test cases you just added and click `Confirm`

## 4. Export the community configuration

As `admin@itb` user, export the community configuration.

### Steps

1. Go to the `Data export` tab in the ITB UI
2. Select `Community configuration`
3. Choose the `FHIR community`
4. Input the password `fhir`
5. Click on the `all` button to select all the data to export
6. Click on 'Export'
7. Give it the name `itb_config.zip`
8. Save the file inside the `config/data` directory in the project root and
   overwrite the existing `itb_config.zip` file

## 5. Build a new sandbox image version

To build the image, simply follow the `building` steps in
the [dist/gitb-ui-fhir-sandbox/README](../dist/gitb-ui-fhir-sandbox/README.md)
file.
