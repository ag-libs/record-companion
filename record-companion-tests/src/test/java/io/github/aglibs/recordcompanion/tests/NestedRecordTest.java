package io.github.aglibs.recordcompanion.tests;

import static org.junit.jupiter.api.Assertions.*;

import io.github.aglibs.recordcompanion.tests.records.Address;
import io.github.aglibs.recordcompanion.tests.records.AddressBuilder;
import io.github.aglibs.recordcompanion.tests.records.Person;
import io.github.aglibs.recordcompanion.tests.records.PersonBuilder;
import org.junit.jupiter.api.Test;

class NestedRecordTest {

  @Test
  void testNestedRecordBuilding() {
    // Test building a Person with nested Address using chained method calls
    Address address =
        AddressBuilder.builder().street("123 Main St").city("New York").zipCode("10001").build();

    Person person = PersonBuilder.builder().name("John Doe").age(30).address(address).build();

    assertEquals("John Doe", person.name());
    assertEquals(30, person.age());
    assertEquals("123 Main St", person.address().street());
    assertEquals("New York", person.address().city());
    assertEquals("10001", person.address().zipCode());
  }

  @Test
  void testNestedFluentUpdate() {
    // Test using the fluent nested update method with chaining
    Person original = new Person("Jane", 25, new Address("456 Oak Ave", "Boston", "02101"));

    Person updated =
        PersonBuilder.with(
            original,
            u -> u.name("Jane Smith").address(au -> au.street("789 Pine St").city("Chicago")));

    assertEquals("Jane Smith", updated.name());
    assertEquals(25, updated.age()); // unchanged
    assertEquals("789 Pine St", updated.address().street()); // updated
    assertEquals("Chicago", updated.address().city()); // updated
    assertEquals("02101", updated.address().zipCode()); // unchanged from original
  }

  @Test
  void testNestedFluentUpdateFromNull() {
    // Test fluent update when nested field is initially null using chaining
    Person personWithNullAddress = new Person("Bob", 35, null);

    Person updated =
        PersonBuilder.with(
            personWithNullAddress,
            u -> u.address(au -> au.street("999 New St").city("Seattle").zipCode("98101")));

    assertEquals("Bob", updated.name());
    assertEquals(35, updated.age());
    assertNotNull(updated.address());
    assertEquals("999 New St", updated.address().street());
    assertEquals("Seattle", updated.address().city());
    assertEquals("98101", updated.address().zipCode());
  }

  @Test
  void testMixedUpdates() {
    // Test mixing direct address assignment and fluent updates
    Address originalAddress = new Address("111 First St", "Miami", "33101");
    Person original = new Person("Alice", 28, originalAddress);

    // First, set a completely new address using direct assignment
    Address newAddress = new Address("222 Second St", "Denver", "80201");
    Person withNewAddress = PersonBuilder.with(original, updater -> updater.address(newAddress));

    assertEquals("222 Second St", withNewAddress.address().street());
    assertEquals("Denver", withNewAddress.address().city());
    assertEquals("80201", withNewAddress.address().zipCode());

    // Then, use fluent update on the new address with chaining
    Person finalUpdate =
        PersonBuilder.with(withNewAddress, u -> u.address(au -> au.zipCode("80202")));

    assertEquals("222 Second St", finalUpdate.address().street()); // unchanged
    assertEquals("Denver", finalUpdate.address().city()); // unchanged
    assertEquals("80202", finalUpdate.address().zipCode()); // updated
  }
}
