package com.secretapp.backend.data.models

import com.secretapp.backend.data.types._

case class User(firstName : String, lastName : String, sex : Sex)

/* case class User(firstName: String, lastName: String, sex: Sex, photo: BitVector)
 * perhaps we don't want to carry this bits through app stacks
 */
