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

import java.sql.Date
import org.devzendo.commondb.DateUtils

case class Transaction(id: Int, accountId: Int, index: Int, amount: Int,
                       isCredit: Boolean, isReconciled: Boolean,
                       origTransactionDate: Date, accountBalance: Int) {
//    val transactionDate = DateUtils.normalise(origTransactionDate)
}