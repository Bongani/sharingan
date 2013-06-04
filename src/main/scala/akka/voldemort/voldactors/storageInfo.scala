package akka.voldemort.voldactors

import java.util.Map
import java.util.HashMap
import voldemort.client.DefaultStoreClient

class storageInfo {
  //clientStore: DefaultStoreClient[Object, Object]
  
  var storageMap: Map[String, DefaultStoreClient[Object, Object]] = new HashMap[String, DefaultStoreClient[Object, Object]];
  
  def getStorage(storageName: String) : DefaultStoreClient[Object, Object] ={
    return storageMap.get(storageName);
  }
  
  def putStorage(storageName: String, storage: DefaultStoreClient[Object, Object]) : Unit ={
    storageMap.put(storageName, storage);
  }
  
  def removeStorage(storageName: String) : Unit ={
    storageMap.remove(storageName);
  }

}