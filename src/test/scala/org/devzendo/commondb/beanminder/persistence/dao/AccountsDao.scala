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

package org.devzendo.commondb.beanminder.persistence.dao

import org.devzendo.commondb.beanminder.persistence.domain.Account

trait AccountsDao {
    /**
     * Find all the accounts sorted by name.
     * @return all the accounts, sorted on name.
     */
    def findAllAccounts(): List[Account]

    /**
     * Save (insert or update) an account.
     * @param account the account to insert or update
     * @return the saved account; if inserted, the id will contain
     * the primary key.
     */
    def saveAccount(account: Account): Account

    /**
     * Delete an account, cascading the deletion of all its
     * transactions.
     * @param account the account to delete
     */
    def deleteAccount(account: Account)
}
