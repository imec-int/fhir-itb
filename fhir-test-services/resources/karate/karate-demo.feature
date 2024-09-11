Feature: Karate Demo Test

  Scenario: Test example
    Given url 'https://jsonplaceholder.typicode.com'
    When path 'posts'
    Then status 200
    And match response[0].id == 1
