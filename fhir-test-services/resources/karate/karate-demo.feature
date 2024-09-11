Feature: Karate Demo Test

  Scenario: Test example
    Given url 'https://jsonplaceholder.typicode.com'
    When path 'posts'
    And method get
    Then status 200
    And match response[0].id == 1
