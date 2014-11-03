package com.secretapp.backend.models.contact

import collection.immutable

@SerialVersionUID(1L)
case class UserContactsListCache(ownerId: Int,
                                 contactsId: immutable.Set[Int],
                                 deletedContactsId: immutable.Set[Int])
