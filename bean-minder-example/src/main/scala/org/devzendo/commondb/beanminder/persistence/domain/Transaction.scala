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

import org.devzendo.commondb.util.{RepresentationType, NormalisedDate}

case class Index(index: Int) extends RepresentationType[Int](index)
case class Amount(amount: Int) extends RepresentationType[Int](amount)
case class AccountBalance(balance: Int) extends RepresentationType[Int](balance)

object CreditDebit extends Enumeration {
    type CreditDebit = Value
    val Credit, Debit = Value
}
object Reconciled extends Enumeration {
    type Reconciled = Value
    val Reconciled, NotReconciled = Value
}
import CreditDebit._
import Reconciled._


object Transaction {
    def apply(amount: Amount, isCredit: CreditDebit,
        isReconciled: Reconciled, transactionDate: NormalisedDate) = {
        new Transaction(-1, -1, Index(-1), amount, isCredit, isReconciled, transactionDate, AccountBalance(-1))
    }
}

case class Transaction(id: Int, accountId: Int, index: Index, amount: Amount,
                       isCredit: CreditDebit, isReconciled: Reconciled,
                       transactionDate: NormalisedDate, accountBalance: AccountBalance) {
    if (amount.toRepresentation <= 0) {
        throw new IllegalArgumentException("Transaction amounts must be positive; " + amount + " is negative")
    }
}
