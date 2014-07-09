package com.secretapp.backend.persist

/**
  * Type that represents an entity with both its Key and Scala record representation
  *
  * It's done in analogue with haskell's persistent (http://hackage.haskell.org/package/persistent-1.3.1.1/docs/Database-Persist-Types.html#t:Entity)
  *
  * @constructor create a persist entity
  * @param key the entity key
  * @param value the Scala record representation
  */

class KeyedEntity[Key](val key: Key)

case class Entity[Key, Value](key: Key, value: Value) extends KeyedEntity(key)
