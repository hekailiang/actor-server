package com.secretapp.backend.models.contact

import collection.immutable

@SerialVersionUID(1L)
case class UserContactsListCache(ownerId: Int, sha1Hash: String, contactsId: immutable.Set[Int])
