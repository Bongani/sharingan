package messages

import voldemort.client.DefaultStoreClient
import voldemort.versioning.Version
import voldemort.versioning.Versioned

class voldemortMessage(operation: String, storeName: String, key: Object, value: Object) {
  def op = operation;
  def cStoreName = storeName;
  def k = key;
  def v = value;
}


class voldemortDirectMessage(operation: String, clientStore: DefaultStoreClient[Object, Object], key: Object, value: Object, version: Version, defaultValue: Versioned[Object], versioned: Versioned[Object]) {
  def op = operation;
  def cStore = clientStore;
  def k = key;
  def v = value;
  def vers = version;
  def defaultVal = defaultValue;  
  def versionedValue = versioned;
}