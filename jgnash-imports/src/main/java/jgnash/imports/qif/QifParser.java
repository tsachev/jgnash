/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.imports.qif;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The QIF format seems to be very broken. Various applications and services
 * export it differently, some have even extended an already broken format to
 * add even more confusion. To make matters worse, the format has changed over
 * time with no indication of what "version" the QIF file is.
 * <p/>
 * This parses through the QIF file using brute force, no fancy parser or
 * tricks. The QIF file is broken enough that it's easier to find problems with
 * the parser when it's easy to step through the code.
 * <p/>
 * The !Option:AutoSwitch and !Clear:AutoSwitch headers do not appear to be used
 * correctly, even by the "creator" of the QIF file format. There seems to be
 * confusion as to it's purpose. The best thing to do is ignore AutoSwitch
 * completely and make an educated guess about the data.
 * <p/>
 * I'm not very happy with this code, but I'm not sure there is a clean solution
 * to parsing QIF files
 * 
 * @author Craig Cavanaugh
 * 
 */
public final class QifParser {

    private String dateFormat = QifUtils.US_FORMAT;

    private boolean debug = false;

    public ArrayList<QifCategory> categories = new ArrayList<>();

    private ArrayList<QifClassItem> classes = new ArrayList<>();

    public ArrayList<QifAccount> accountList = new ArrayList<>();

    private ArrayList<QifSecurity> securities = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(QifParser.class.getName());

    public QifParser(String dateFormat) {
        setDateFormat(dateFormat);
    }

    /**
     * Tests if the source string starts with the prefix string. Case is
     * ignored.
     * 
     * @param source
     *            the source String.
     * @param prefix
     *            the prefix String.
     * @return true, if the source starts with the prefix string.
     */
    private static boolean startsWith(final String source, final String prefix) {
        return prefix.length() <= source.length() && source.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    void setDateFormat(final String dateFormat) {
        if (dateFormat != null) {
            if (dateFormat.equals(QifUtils.US_FORMAT) || dateFormat.equals(QifUtils.EU_FORMAT)) {
                this.dateFormat = dateFormat;
            }
        }
    }

    void parseFullFile(final File file) throws NoAccountException {
        parseFullFile(file.getAbsolutePath());
    }

    void parsePartialFile(final File file) {
        parsePartialFile(file.getAbsolutePath());
    }

    private void parseFullFile(final String fileName) throws NoAccountException {
        
        boolean accountFound = true;

        try (QifReader in = new QifReader(new FileReader(fileName))) {

            String line = in.readLine();
            
            while (line != null) {                                                                               
                if (startsWith(line, "!Type:Class")) {
                    parseClassList(in);
                } else if (startsWith(line, "!Type:Cat")) {
                    parseCategoryList(in);
                } else if (startsWith(line, "!Account")) {
                    parseAccount(in);
                } else if (startsWith(line, "!Type:Memorized")) {
                    parseMemorizedTransactions(in);
                } else if (startsWith(line, "!Type:Security")) {
                    parseSecurity(in);
                } else if (startsWith(line, "!Type:Prices")) {
                    parsePrice(in);
                } else if (startsWith(line, "!Type:Bank")) { // QIF from an online bank statement... assumes the account is known                  
                    accountFound = false;
                    break;                  
                } else if (startsWith(line, "!Type:CCard")) { // QIF from an online credit card statement
                    accountFound = false;
                    break;                    
                } else if (startsWith(line, "!Type:Oth")) { // QIF from an online credit card statement
                    accountFound = false;
                    break;                  
                } else if (startsWith(line, "!Type:Cash")) { // Partial QIF export
                    accountFound = false;
                    break;                    
                } else if (startsWith(line, "!Option:AutoSwitch")) {
                    logger.info("Consuming !Option:AutoSwitch");
                } else if (startsWith(line, "!Clear:AutoSwitch")) {
                    logger.info("Consuming !Clear:AutoSwitch");
                } else {
                    System.out.println("Error: " + line);
                }
                line = in.readLine();
            }          
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Could not find file: {0}", fileName);
        } catch (IOException e) {
            logger.log(Level.SEVERE, null, e);
        }
        
        if (!accountFound) {
            throw new NoAccountException();
        }
    }

    private boolean parsePartialFile(final String fileName) {

        try (QifReader in = new QifReader(new FileReader(fileName))) {

            String peek = in.peekLine();
            if (startsWith(peek, "!Type:")) {
                QifAccount acc = new QifAccount(); // "unknown" holding account
                if (parseAccountTransactions(in, acc)) {
                    accountList.add(acc);
                    if (debug) {
                        System.out.println("*** Added account ***");
                    }
                   
                    return true; // only look for transactions for one account
                }
                System.err.println("parseAccountTransactions: error");
            }
          
        } catch (FileNotFoundException fne) {
            logger.log(Level.WARNING, "Could not find file: {0}", fileName);
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, null, ioe);
        }
        return false;
    }

    private boolean parseAccount(final QifReader in) {
        logger.entering(this.getClass().getName(), "parseAccount", in);

        String line;
        QifAccount acc = new QifAccount();

        boolean result = false;

        try {
            line = in.readLine();
            while (line != null) {
                if (line.startsWith("N")) {
                    acc.name = line.substring(1);
                } else if (line.startsWith("T")) {
                    acc.type = line.substring(1);
                } else if (line.startsWith("D")) {
                    acc.description = line.substring(1);
                } else if (line.startsWith("L")) {
                    acc.creditLimit = line.substring(1);
                } else if (line.startsWith("/")) {
                    acc.statementBalanceDate = line.substring(1);
                } else if (line.startsWith("$")) {
                    acc.statementBalance = line.substring(1);
                } else if (line.startsWith("X")) {
                    // must be GnuCashToQIF... not sure what it is??? ignore it.
                } else if (line.startsWith("^")) {
                    String peek = in.peekLine();
                    if (peek == null) { // end of the file in empty account list
                        accountList.add(acc);
                        result = true;
                        break;
                    }
                    if (startsWith(peek, "!Account")) {
                        // must be in an account list, no transaction data here
                        accountList.add(acc);
                        acc = new QifAccount();
                        in.readLine(); // eat the line since we only peeked at it
                    } else if (startsWith(peek, "!Type:Memor")) {
                        accountList.add(acc);
                        result = true;
                        break;
                    } else if (startsWith(peek, "!Type:Invst")) { // investment transactions follow
                        logger.fine("Found investment transactions");
                        /* Search for a duplicate account generated by the account list and
                         * use it if possible.  Qif exports will output a list of empty accounts
                         * and then follow up with the same account and it's transactions
                         */
                        QifAccount dup = searchForDuplicate(acc);
                        if (dup != null) {
                            acc = dup; // trade for the duplicate already existing in the list
                        }

                        if (parseInvestmentAccountTransactions(in, acc)) {
                            if (dup == null) {
                                accountList.add(acc); // only add if not a duplicate
                            }
                            logger.finest("Added Qif Account");
                            result = true;
                            break; // exit here, the outer loop will catch the next account if it exists
                        }
                        acc = new QifAccount();
                    } else if (startsWith(peek, "!Type:Prices")) { // security prices, jump out
                        logger.fine("Found commodity price; jump out");
                        result = true;
                        break;
                    } else if (startsWith(peek, "!Type:")) {
                        // must be transactions that follow
                        logger.fine("Found bank transactions");

                        /* Search for a duplicate account generated by the account list and
                         * use it if possible.  Qif exports will output a list of empty accounts
                         * and then follow up with the same account and it's transactions
                         */
                        QifAccount dup = searchForDuplicate(acc);
                        if (dup != null) {
                            acc = dup; // trade for the duplicate already existing in the list
                        }

                        if (parseAccountTransactions(in, acc)) {
                            if (dup == null) {
                                accountList.add(acc); // only add if not a duplicate
                            }
                            logger.finest("Added Qif Account");
                            result = true;
                            break; // exit here, the outer loop will catch the next account if it exists
                        }
                        acc = new QifAccount();
                    } else if (startsWith(peek, "!Clear:Auto")) {
                        in.readLine(); // the broken AutoSwitch.... eat the line
                        accountList.add(acc);
                        result = true;
                        break;
                    } else if (startsWith(peek, "!")) {
                        // something weird, assume in empty account list
                        accountList.add(acc);
                        result = true;
                        break;
                    } else {
                        // must be in an account list using AutoSwitch
                        accountList.add(acc);
                        acc = new QifAccount();
                    }
                } else {
                    result = false;
                    break;
                }
                line = in.readLine();
            }
        } catch (IOException e) {
            result = false;
        }

        logger.exiting(this.getClass().getName(), "parseAccount", result);
        return result;
    }

    QifAccount searchForDuplicate(final QifAccount acc) {
        String name = acc.name;
        String type = acc.type;
        String description = acc.description; // assume non-null description

        assert description != null;

        if (name == null || type == null) {
            logger.log(Level.SEVERE, "Invalid account: \n{0}", acc.toString());
            return null;
        }

        /* Investment account types are not consistent in Quicken export.... buggy software.
         * A Type of "Invst" is used as a generic when listing the transactions in the
         * investment account.
         * TODO "Port" is only one type.... need to discover other types
         */
        if (type.equals("Invst") || type.equals("Port")) {
            for (QifAccount a : accountList) {
                if (a.name.equals(name) && a.description.equals(description)) {
                    logger.fine("Matched a duplicate account");
                    return a;
                }
            }
        } else {
            for (QifAccount a : accountList) {
                if (a.name.equals(name) && a.type.equals(type) && a.description.equals(description)) {
                    logger.fine("Matched a duplicate account");
                    return a;
                }
            }
        }
        return null;
    }

    // TODO strip out investment account transaction checks
    private boolean parseAccountTransactions(final QifReader in, final QifAccount acc) {

        String line;
        QifTransaction tran = new QifTransaction();
        try {
            line = in.readLine();
            while (line != null) {
                if (startsWith(line, "!Type:")) {
                    if (startsWith(line, "!Type:Invst") || startsWith(line, "!Type:Invst")) {

                        tran.type = line.substring(1);
                    } else if (startsWith(line, "!Type:Memor")) {
                        in.reset();
                        return true;
                    } else {

                        tran.type = line.substring(1);
                    }
                } else if (line.startsWith("D")) {
                    /* Preserve the original unparsed date so that it may be
                     * reevaluated at a later time. */
                    tran.oDate = line.substring(1);
                    tran.date = QifUtils.parseDate(tran.oDate, dateFormat);
                } else if (line.startsWith("U")) {
                    tran.U = line.substring(1);
                } else if (line.startsWith("T")) {
                    tran.amount = QifUtils.parseMoney(line.substring(1));
                } else if (line.startsWith("C")) {
                    tran.status = line.substring(1);
                } else if (line.startsWith("P")) {
                    tran.payee = line.substring(1);
                } else if (line.startsWith("L")) {
                    tran.category = line.substring(1);
                } else if (line.startsWith("N")) {
                    tran.number = line.substring(1);
                } else if (line.startsWith("M")) {
                    tran.memo = line.substring(1);
                } else if (line.startsWith("A")) {
                    tran.addAddressLine(line.substring(1));
                } else if (line.startsWith("I")) {
                    tran.price = line.substring(1);
                } else if (line.startsWith("^")) {
                    acc.addTransaction(tran);
                    if (debug) {
                        System.out.println("*** Added a Transaction ***");
                    }
                    tran = new QifTransaction();
                } else if (startsWith(line, "!Account")) {
                    in.reset();
                    return true;
                } else if (startsWith(line, "!Type:Prices")) { // fund prices... jump out
                    in.reset();
                    return true;
                } else if (line.charAt(0) == 'S' || line.charAt(0) == 'E' || line.charAt(0) == '$'
                        || line.charAt(0) == '%') {
                    // doing a split transaction
                    in.reset();
                    QifSplitTransaction split = parseSplitTransaction(in);
                    if (split != null) {
                        tran.addSplit(split);
                        if (debug) {
                            System.out.println("*** Added a Split Transaction ***");
                        }
                    }
                } else if (line.charAt(0) == '$') {
                    tran.amountTrans = line.substring(1);
                } else {
                    logger.log(Level.SEVERE, "Unknown field: {0}", line);
                }
                in.mark();
                line = in.readLine();
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean parseInvestmentAccountTransactions(final QifReader in, final QifAccount acc) {

        boolean result = true;

        logger.entering(this.getClass().getName(), "parseInvestmentAccountTransactions", acc);
        String line;
        QifTransaction tran = new QifTransaction();
        try {
            line = in.readLine();
            while (line != null) {
                if (startsWith(line, "!Type:Invst")) { // TODO Bogus check?
                    tran.type = line.substring(1);
                } else if (startsWith(line, "!Type:Memor")) {
                    in.reset();
                    result = true;
                    break;
                } else if (startsWith(line, "!Type:Prices")) { // fund prices... jump out
                    logger.fine("Found commodity prices; jumping out");
                    in.reset();
                    result = true;
                    break;
                } else if (line.startsWith("D")) {
                    /* Preserve the original unparsed date so that it may be
                     * reevaluated at a later time. */
                    tran.oDate = line.substring(1);
                    tran.date = QifUtils.parseDate(tran.oDate, dateFormat);
                } else if (line.startsWith("U")) {
                    tran.U = line.substring(1);
                } else if (line.startsWith("T")) {
                    tran.amount = QifUtils.parseMoney(line.substring(1));
                } else if (line.startsWith("C")) {
                    tran.status = line.substring(1);
                } else if (line.startsWith("P")) {
                    tran.payee = line.substring(1);
                } else if (line.startsWith("L")) {
                    tran.category = line.substring(1);
                } else if (line.startsWith("N")) { // trans type for inv accounts
                    tran.number = line.substring(1);
                } else if (line.startsWith("M")) {
                    tran.memo = line.substring(1);
                } else if (line.startsWith("A")) {
                    tran.addAddressLine(line.substring(1));
                } else if (line.startsWith("Y")) {
                    tran.security = line.substring(1);
                } else if (line.startsWith("I")) {
                    tran.price = line.substring(1);
                } else if (line.startsWith("Q")) {
                    tran.quantity = line.substring(1);
                } else if (line.charAt(0) == '$') { // must check before split trans checks... Does Quicken allow for split investment transactions?
                    tran.amountTrans = line.substring(1);
                } else if (line.startsWith("^")) {
                    acc.addTransaction(tran);
                    logger.finest("*** Added an investment transaction ***");
                    tran = new QifTransaction();
                } else if (startsWith(line, "!Account")) {
                    in.reset();
                    result = true;
                    break;
                } else if (line.charAt(0) == 'S' || line.charAt(0) == 'E' || line.charAt(0) == '$'
                        || line.charAt(0) == '%') {
                    // doing a split transaction
                    in.reset();
                    QifSplitTransaction split = parseSplitTransaction(in);
                    if (split != null) {
                        tran.addSplit(split);
                        logger.fine("*** Added a Split Transaction ***");
                    }
                } else {
                    logger.log(Level.SEVERE, "Unknown field: {0}", line);
                }
                in.mark();
                line = in.readLine();
            }
        } catch (IOException e) {
            result = false;
        }

        logger.exiting(this.getClass().getName(), "parseInvestmentAccountTransactions", result);
        return result;
    }

    private static QifSplitTransaction parseSplitTransaction(final QifReader in) {
        boolean category = false;
        boolean memo = false;
        boolean amount = false;
        boolean percentage = false;
        String line;
        QifSplitTransaction split = new QifSplitTransaction();
        try {
            line = in.readLine();
            while (line != null) {
                if (line.startsWith("S")) {
                    if (category) {
                        in.reset();
                        return split;
                    }
                    category = true;
                    split.category = line.substring(1);
                } else if (line.startsWith("E")) {
                    if (memo) {
                        in.reset();
                        return split;
                    }
                    memo = true;
                    split.memo = line.substring(1);
                } else if (line.startsWith("$")) {
                    if (amount) {
                        in.reset();
                        return split;
                    }
                    amount = true;
                    split.amount = QifUtils.parseMoney(line.substring(1));
                } else if (line.startsWith("%")) {
                    if (percentage) {
                        in.reset();
                        return split;
                    }
                    percentage = true;
                    split.percentage = line.substring(1);
                } else if (line.startsWith("^")) {
                    in.reset();
                    return split;
                } else {
                    in.reset();
                    return null;
                }
                in.mark();
                line = in.readLine();
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Just eats the data, it's not useful right now
     * 
     * @param in
     *            <code>QifReader</code>
     */
    private void parseMemorizedTransactions(final QifReader in) {
        if (debug) {
            System.out.println("*** Start: parseMemorizedTransactions ***");
        }
        String line;
        try {
            line = in.readLine();
            while (line != null) {
                if (line.startsWith("K")) {
                    // munch until all of them are gone
                    line = in.readLine();
                    if (line.charAt(0) == '^') {
                        String peek = in.peekLine();
                        if (peek == null) {
                            return;
                        } else if (!peek.startsWith("K")) {
                            break;
                        }
                    }

                } else {
                    in.reset();
                    return;
                }
                in.mark();
                line = in.readLine();
            }
        } catch (IOException e) {
            logger.warning(e.getLocalizedMessage());
        }
    }

    private boolean parseCategoryList(final QifReader in) {
        String line;
        QifCategory cat = new QifCategory();
        try {
            line = in.readLine();
            while (line != null) {
                if (line.startsWith("N")) {
                    cat.name = line.substring(1);
                } else if (line.startsWith("D")) {
                    cat.description = line.substring(1);
                } else if (line.startsWith("T")) {
                    cat.taxRelated = true;
                } else if (line.startsWith("I")) {
                    cat.type = "I";
                } else if (line.startsWith("E")) {
                    cat.type = "E";
                } else if (line.startsWith("B")) {
                    cat.budgetAmount = line.substring(1);
                } else if (line.startsWith("R")) {
                    cat.taxSchedule = line.substring(1);
                } else if (line.startsWith("^")) { // a complete category item
                    categories.add(cat); // add it to the list
                    cat = new QifCategory(); // start a new one
                    in.mark(); // next line might be end of list
                } else if (line.startsWith("!")) { // done with category list
                    in.reset(); // give the line back
                    return true; // a good return
                } else {
                    System.out.println("Error: " + line);
                    return false;
                }
                line = in.readLine();
            }
            return true; // reached the end of the file, a good return
        } catch (IOException e) {
            return false;
        }
    }

    private boolean parseClassList(final QifReader in) {
        String line;
        QifClassItem clas = new QifClassItem();
        try {
            line = in.readLine();
            while (line != null) {
                if (line.startsWith("N")) {
                    clas.name = line.substring(1);
                } else if (line.startsWith("D")) {
                    clas.description = line.substring(1);
                } else if (line.startsWith("^")) { // end of a class item
                    classes.add(clas); // add it to the list
                    clas = new QifClassItem(); // start a new one
                    in.mark(); // next line might be end of the list
                } else if (line.startsWith("!")) { // done with the class list
                    in.reset(); // give the line back
                    return true; // a good return
                } else {
                    System.out.println("Error: " + line);
                    return false;
                }
                line = in.readLine();
            }
            return true; // reached the end of the file, a good return
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * So far, I haven't see a security as part of a list, but it is supported
     * just in case there is another "variation" of the format
     * 
     * @param in
     *            <code>QifReader</code>
     * @return true if successful
     */
    private boolean parseSecurity(final QifReader in) {
        String line;
        QifSecurity sec = new QifSecurity();

        try {
            line = in.readLine();
            while (line != null) {
                if (line.startsWith("N")) {
                    sec.name = line.substring(1);
                } else if (line.startsWith("D")) {
                    sec.description = line.substring(1);
                } else if (line.startsWith("T")) {
                    sec.type = line.substring(1);
                } else if (line.startsWith("S")) {
                    sec.symbol = line.substring(1);
                } else if (line.startsWith("^")) {
                    securities.add(sec);
                    sec = new QifSecurity();
                    in.mark();
                } else if (line.startsWith("!")) {
                    in.reset();
                    return true;
                } else {
                    System.out.println("Error: " + line);
                    return false;
                }
                line = in.readLine();
            }
            return true; // reached the end of the file, a good return
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Price data in QIF file is not very informative.... ignore it for now
     * 
     * @param in
     *            <code>QifReader</code>
     * @return true if successful
     */
    private boolean parsePrice(final QifReader in) {
        logger.entering(this.getClass().getName(), "parsePrice");

        boolean result = true;

        String line;
        try {
            line = in.readLine();
            while (line != null) {
                if (line.startsWith("^")) {
                    result = true;
                    break;
                }
                line = in.readLine();
            }
        } catch (IOException e) {
            result = false;
        }

        logger.exiting(this.getClass().getName(), "parsePrice", result);
        return result;
    }

    public void dumpStats() {
        System.out.println("Num Classes :" + classes.size());
        System.out.println("Num Categories :" + categories.size());
        System.out.println("Num Securities :" + securities.size());
        System.out.println("Num Accounts :" + accountList.size());

        int count = accountList.size();
        for (int i = 0; i < count; i++) {
            QifAccount acc = accountList.get(i);
            System.out.println("Account " + (i + 1) + " " + acc.name);
            int size = acc.items.size();
            System.out.println("    Num Transactions :" + size);
            for (int j = 0; j < size; j++) {
                QifTransaction tran = acc.items.get(j);
                System.out.println("        Transaction " + (j + 1) + " " + tran.payee);
                System.out.println("            Num Splits :" + tran.splits.size());
                for (int k = 0; k < tran.splits.size(); k++) {
                    System.out.println("                Split " + (k + 1) + " " + tran.splits.get(k).memo);
                }
            }
        }
    }

    static class QifSecurity {

        String name;

        String description;

        String symbol;

        String type;

    }

    static class QifClassItem {

        String name;

        String description;

    }
}
