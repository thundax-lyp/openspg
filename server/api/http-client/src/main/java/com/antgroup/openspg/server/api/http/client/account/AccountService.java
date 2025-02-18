/*
 * Copyright 2023 OpenSPG Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package com.antgroup.openspg.server.api.http.client.account;

import com.antgroup.openspg.server.api.facade.Paged;
import com.antgroup.openspg.server.common.model.account.Account;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/** get account from hr interface(ant) */
public interface AccountService {

    /**
     * get a login user from buc
     *
     * @return Account
     */
    Account getLoginUser();

    /**
     * get accounts by part of account info
     *
     * @param keyword keyword
     * @return List
     */
    List<Account> getAccountByKeyword(String keyword);

    /**
     * get account info by userNo
     *
     * @param userNo userNo
     * @return Account
     */
    Account getByUserNo(String userNo);

    /**
     * get account info by userNo with private info
     *
     * @param userNo userNo
     * @return Account
     */
    Account getWithPrivateByUserNo(String userNo);

    /**
     * create a new account
     *
     * @param account account
     * @return Integer
     */
    Integer create(Account account);

    /**
     * update password
     *
     * @param account account
     * @return Integer
     */
    Integer updatePassword(Account account);

    /**
     * delete account
     *
     * @param workNo workNo
     * @return Integer
     */
    Integer deleteAccount(String workNo);

    /**
     * get account list
     *
     * @param account account
     * @param page    page
     * @param size    size
     * @return Paged
     */
    Paged<Account> getAccountList(String account, Integer page, Integer size);

    /**
     * get sha256Hex password
     *
     * @param password password
     * @param salt     salt
     * @return String
     */
    String getSha256HexPassword(String password, String salt);

    Account getCurrentAccount(Cookie[] cookies) throws IOException;

    boolean login(Account account, HttpServletResponse response);

    String logout(String workNo, String redirectUrl);

    /**
     * update user config
     *
     * @param account account
     * @param cookies cookies
     * @return int
     */
    int updateUserConfig(Account account, Cookie[] cookies);
}
