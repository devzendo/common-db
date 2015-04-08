/**
 * Copyright (C) 2008-2012 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.devzendo.commondb.beanminder.persistence.domain

import org.devzendo.commondb.util.RepresentationType

case class AccountName(name: String) extends RepresentationType[String](name)
case class BankName(name: String) extends RepresentationType[String](name)
case class AccountCode(code: String) extends RepresentationType[String](code)
case class InitialBalance(balance: Int) extends RepresentationType[Int](balance)
case class CurrentBalance(balance: Int) extends RepresentationType[Int](balance)

object Account {
    def apply(name: AccountName, withBank: BankName, accountCode: AccountCode,
              initialBalance: InitialBalance): Account = {
        new Account(-1, name, withBank, accountCode, initialBalance, CurrentBalance(initialBalance.toRepresentation))
    }
}

case class Account(id: Int, name: AccountName, withBank: BankName,
                   accountCode: AccountCode,
                   initialBalance: InitialBalance, currentBalance: CurrentBalance)
