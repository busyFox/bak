package com.funbridge.server.player.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.ws.player.WSPlayerProfile;
import com.gotogames.common.tools.StringTools;

import javax.persistence.*;
import java.sql.Date;
import java.util.Calendar;

@Entity
@Table(name = "player")
@NamedQueries({
        @NamedQuery(name = "player.list", query = "select p from Player p"),
        @NamedQuery(name = "player.getForMail", query = "select p from Player p where p.mail=:mail"),
        @NamedQuery(name = "player.getForCert", query = "select p from Player p where p.cert=:cert"),
        @NamedQuery(name = "player.getForNickname", query = "select p from Player p where p.nickname=:nickname"),
        @NamedQuery(name = "player.getForCountryCode", query = "select p.ID from Player p where p.countryCode in(:countriesCode) and p.lastConnectionDate > :lastConnDate "),
        @NamedQuery(name = "player.searchForMail", query = "select p from Player p where p.mail like :mail"),
        @NamedQuery(name = "player.searchForCert", query = "select p from Player p where p.cert like :cert"),
        @NamedQuery(name = "player.searchForPseudo", query = "select p from Player p where p.nickname like :pseudo"),
        @NamedQuery(name = "player.count", query = "select count(p) from Player p"),
        @NamedQuery(name = "player.findLastByDevice", query = "select p from Player p join Device d on p.id = d.lastPlayerID where d.deviceID = :deviceId and d.type = :deviceType and p.nickname not like 'DELETED_%'")
})
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private long ID;

    @Column(name = "nickname", length = 255)
    private String nickname = "";

    @Column(name = "password", length = 255)
    private String password = "";

    @Column(name = "cert", length = 255)
    private String cert = "";

    @Column(name = "type")
    private int type = 0;

    @Column(name = "mail", length = 255)
    private String mail = "";

    @Column(name = "lang", length = 20, nullable = false)
    private String lang = "en";

    @Column(name = "display_lang", length = 10)
    private String displayLang;

    @Column(name = "creation_date", nullable = false)
    private long creationDate = 0;

    @Column(name = "last_connection_date", nullable = false)
    private long lastConnectionDate = 0;

    @Column(name = "credit_amount", nullable = false)
    private int creditAmount = 0;

    @Column(name = "day_credit_amount", nullable = false)
    private int dayCreditAmount = 0;

    @Column(name = "date_last_credit_update")
    private long dateLastCreditUpdate = 0;

    @Column(name = "nb_played_deals")
    private int nbPlayedDeals = 0;

    @Column(name = "firstname")
    private String firstName = "";

    @Column(name = "lastname")
    private String lastName = "";

    @Column(name = "country_code")
    private String countryCode = "";

    @Column(name = "display_country_code")
    private String displayCountryCode = "";

    @Column(name = "town")
    private String town = "";

    @Column(name = "description")
    private String description = "";

    @Column(name = "birthday")
    private Date birthday = null;

    @Column(name = "avatar_present")
    private boolean avatarPresent = false;

    @Column(name = "settings")
    private String settings = null;

    @Column(name = "convention_profile")
    private int conventionProfile = 0;

    @Column(name = "sex")
    private int sex = Constantes.PLAYER_SEX_NOT_DEFINED;

    @Column(name = "subscription_expiration_date")
    private long subscriptionExpirationDate = 0;

    @Column(name = "credentials_init_mask")
    private int credentialsInitMask = 0;

    @Transient
    private boolean trial = false;

    public boolean isTrial() {
        return trial;
    }

    public void setTrial(boolean value) {
        this.trial = value;
    }

    public long getID() {
        return ID;
    }

    public void setID(long iD) {
        ID = iD;
    }

    @Column(name="friend_flag")
    private int friendFlag = Constantes.FRIEND_MASK_NOTIFICATION_DUEL;

    public String getNickname() {
        if (isDisabled()) {
            return ContextManager.getTextUIMgr().getTextUIForLang(Constantes.PLAYER_DELETED, lang);
        }

        return nickname;
    }

    public String getNicknameDeactivated() {
        if (isDisabled()) {
            return Constantes.PLAYER_DISABLE_PATTERN;
        }
        return nickname;
    }

    public boolean isDisabled() {
        return nickname != null && nickname.startsWith(Constantes.PLAYER_DISABLE_PATTERN);
    }

    public void setNickname(String value) {
        if (value != null) {
            if (value.length() > Constantes.PLAYER_PSEUDO_MAX_LENGTH) {
                value = value.substring(0, Constantes.PLAYER_PSEUDO_MAX_LENGTH);
            }
        }
        this.nickname = value;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String value) {
        if (value != null && !value.equals(Constantes.VALUE_TEST)) {
            value = value.toLowerCase();
            if (value.length() > Constantes.PLAYER_LANG_MAX_LENGTH) {
                value = value.substring(0, Constantes.PLAYER_LANG_MAX_LENGTH);
            }
            this.lang = value;
        }
    }

    public String getDisplayLang() {
        if (displayLang == null) displayLang = ContextManager.getPlayerMgr().generateDisplayLang(lang);
        return displayLang;
    }

    public void setDisplayLang(String value) {
        if (value != null) {
            value = value.toLowerCase();
            if (value.length() > Constantes.PLAYER_LANG_MAX_LENGTH) {
                value = value.substring(0, Constantes.PLAYER_LANG_MAX_LENGTH);
            }
        }
        // ???? zh ??
        this.displayLang = "zh";
    }

    public String getPassword() {
        return password;
    }

    public boolean checkPassword(String value) {
        if (value != null && password != null) {
            return password.equalsIgnoreCase(value);
        }
        return false;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String value) {
        if (value != null) {
            if (value.length() > Constantes.PLAYER_MAIL_MAX_LENGTH) {
                value = value.substring(0, Constantes.PLAYER_MAIL_MAX_LENGTH);
            }
        }
        this.mail = value;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCreditAmount() {
        return this.creditAmount;
    }

    public int getDayCreditAmount() {return this.dayCreditAmount;}

    public int getTotalCreditAmount(){return  creditAmount + dayCreditAmount;}

    /**
     * Do not used this method => prefer incrementCreditAmount or decrementCreditAmount.
     * The date of last credit update is not set !
     *
     * @param creditAmount
     */
    public void setCreditAmount(int creditAmount) {
        this.creditAmount = creditAmount;
    }

    /**
     * Do not used this method => prefer incrementDayCreditAmount or decrementDayCreditAmount.
     * The date of last credit update is not set !
     *
     * @param dayCreditAmount
     */
    public void setDayCreditAmount(int dayCreditAmount) {
        this.dayCreditAmount = dayCreditAmount;
    }

    /**
     * add the amount to credit amount. Set date of last credit update to now.
     *
     * @param amount
     */
    public void incrementCreditAmount(int amount) {
        this.creditAmount += amount;
        setDateLastCreditUpdate(System.currentTimeMillis());
    }

    /**
     * Remove amount credit. If subscription is valid, amount credit is not changed
     *
     * @param amount
     */
    public void decrementCreditAmount(int amount) {
        // decrement credit only if date expiration of subscription is before now
        if (!isDateSubscriptionValid()) {
            if (dayCreditAmount - amount >= 0){
                this.dayCreditAmount -= amount;
            } else {
                int tmp = amount - this.dayCreditAmount;

                if (creditAmount > tmp) {
                    this.creditAmount -= tmp;
                } else {
                    creditAmount = 0;
                }
            }
        }
    }

    public long getDateLastCreditUpdate() {
        return dateLastCreditUpdate;
    }

    public void setDateLastCreditUpdate(long dateLastCreditUpdate) {
        this.dateLastCreditUpdate = dateLastCreditUpdate;
    }

    public void incrementNbDealPlayed(int amount) {
        this.nbPlayedDeals += amount;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String value) {
        if (value != null) {
            if (value.length() > Constantes.PLAYER_FIRSTNAME_MAX_LENGTH) {
                value = value.substring(0, Constantes.PLAYER_FIRSTNAME_MAX_LENGTH);
            }
        }
        this.firstName = value;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String value) {
        if (value != null) {
            if (value.length() > Constantes.PLAYER_LASTNAME_MAX_LENGTH) {
                value = value.substring(0, Constantes.PLAYER_LASTNAME_MAX_LENGTH);
            }
        }
        this.lastName = value;
    }

    public String getDisplayCountryCode() {
        return displayCountryCode;
    }

    public void setDisplayCountryCode(String displayCountryCode) {
        // ????
        if (displayCountryCode.compareTo("CN") == 0 || displayCountryCode.compareTo("Haiwai") == 0){
            this.displayCountryCode = displayCountryCode;
        } else{
            this.displayCountryCode = "Haiwai";
        }
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String value) {
        if (value != null) {
            if (!value.equals(Constantes.VALUE_TEST)) {
                if (value.length() > Constantes.PLAYER_COUNTRY_MAX_LENGTH) {
                    value = value.substring(0, Constantes.PLAYER_COUNTRY_MAX_LENGTH);
                }
                this.countryCode = value;
            }
        } else {
            this.countryCode = value;
        }
        // ????
        if (countryCode.compareTo("CN") != 0 && countryCode.compareTo("Haiwai") != 0){
            this.countryCode = "Haiwai";
        }
    }

    public String getTown() {
        return town;
    }

    public void setTown(String value) {
        if (value != null) {
            if (value.length() > Constantes.PLAYER_TOWN_MAX_LENGTH) {
                value = value.substring(0, Constantes.PLAYER_TOWN_MAX_LENGTH);
            }
        }
        this.town = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        if (value != null) {
            if (value.length() > Constantes.PLAYER_DESCRIPTION_MAX_LENGTH) {
                value = value.substring(0, Constantes.PLAYER_DESCRIPTION_MAX_LENGTH - 3);
                value += "...";
            }
        }
        this.description = value;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public void setBirthday(long val) {
        if (val == 0) {
            this.birthday = null;
        } else {
            this.birthday = new Date(val);
        }
    }

    public boolean isAvatarPresent() {
        return avatarPresent;
    }

    public void setAvatarPresent(boolean avatarPresent) {
        this.avatarPresent = avatarPresent;
    }

    /**
     * Transform to player profile.
     */
    public WSPlayerProfile toWSPlayerProfile(long playerAsk, String newSerie) {
        WSPlayerProfile profile = new WSPlayerProfile();
        profile.avatarPresent = isAvatarPresent();
        if (birthday == null) {
            profile.birthdate = 0;
        } else {
            profile.birthdate = birthday.getTime();
        }
        profile.countryCode = displayCountryCode;
        profile.creationDate = creationDate;
        profile.description = description;
        profile.firstName = firstName;
        profile.lastName = lastName;
        profile.sex = sex;
        profile.town = town;
        profile.serie = newSerie;
        profile.nbPlayedDeals = nbPlayedDeals;
        return profile;
    }

    /**
     * Update all fields for profile:
     * avatarPresent, countryCode, dateBirthday, description,
     * firstName, lastName,	mail, password,
     * pseudo, town, isMailCertif
     * mailNewsletter, mailBirthday, mailReport ...
     *
     * @param p
     */
    public void updateProfileFields(Player p) {
        if (p != null) {
            avatarPresent = p.avatarPresent;
            countryCode = p.countryCode;
            displayCountryCode = p.displayCountryCode;
            birthday = p.birthday;
            description = StringTools.removeNonBMPCharacters(p.description, "?");
            firstName = StringTools.removeNonBMPCharacters(p.firstName, "?");
            lastName = StringTools.removeNonBMPCharacters(p.lastName, "?");
            sex = p.sex;
            mail = p.mail;
            password = p.password;
            nickname = p.nickname;
            town = StringTools.removeNonBMPCharacters(p.town, "?");
            settings = p.settings;
            conventionProfile = p.conventionProfile;
            creationDate = p.creationDate;
            friendFlag = p.friendFlag;
            displayLang = p.displayLang;
        }
    }

    /**
     * Update all fields for credit
     * bonusCreditFlag,	creditAmount, dateBonusBirthday, dateLastCreditUpdate
     *
     * @param p
     */
    public void updateCreditFields(Player p) {
        if (p != null) {
            creditAmount = p.creditAmount;
            dayCreditAmount = p.dayCreditAmount;
            dateLastCreditUpdate = p.dateLastCreditUpdate;
            subscriptionExpirationDate = p.subscriptionExpirationDate;
        }
    }

    /**
     * Update all fields when playing deal ...
     * nbDealPlayed, lastPeriodIdPlayed
     *
     * @param p
     */
    public void updateDealFields(Player p) {
        if (p != null) {
            nbPlayedDeals = p.nbPlayedDeals;
        }
    }


    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public int getConventionProfile() {
        return conventionProfile;
    }

    public void setConventionProfile(int conventionProfile) {
        this.conventionProfile = conventionProfile;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int val) {
        this.sex = val;
    }


    /**
     * Add the duration of nb month for the expiration subscription date
     *
     * @param nbMonthDuration
     */
    public void addSubscriptionDuration(int nbMonthDuration) {
        if (nbMonthDuration > 0) {
            Calendar calSubscriptionExpiration = Calendar.getInstance();
            if (isDateSubscriptionValid()) {
                calSubscriptionExpiration.setTimeInMillis(subscriptionExpirationDate);
            }
            calSubscriptionExpiration.add(Calendar.MONTH, nbMonthDuration);
            calSubscriptionExpiration.set(Calendar.HOUR_OF_DAY, 23);
            calSubscriptionExpiration.set(Calendar.MINUTE, 59);
            calSubscriptionExpiration.set(Calendar.SECOND, 59);
            subscriptionExpirationDate = calSubscriptionExpiration.getTimeInMillis();
        }
    }

    /**
     * Add the duration of nb day for the expiration subscription date
     *
     * @param nbDayDuration
     */
    public void addSubscriptionDurationDay(int nbDayDuration) {
        if (nbDayDuration > 0) {
            Calendar calSubscriptionExpiration = Calendar.getInstance();
            if (isDateSubscriptionValid()) {
                calSubscriptionExpiration.setTimeInMillis(subscriptionExpirationDate);
            }
            calSubscriptionExpiration.add(Calendar.DAY_OF_YEAR, nbDayDuration);
            calSubscriptionExpiration.set(Calendar.HOUR_OF_DAY, 23);
            calSubscriptionExpiration.set(Calendar.MINUTE, 59);
            calSubscriptionExpiration.set(Calendar.SECOND, 59);
            subscriptionExpirationDate = calSubscriptionExpiration.getTimeInMillis();
        }
    }

    /**
     * Check if dateSubscriptionExpiration is > current time.
     *
     * @return false if date is 0 or expired
     */
    public boolean isDateSubscriptionValid() {
        if (subscriptionExpirationDate == 0) {
            return false;
        }
        return subscriptionExpirationDate >= System.currentTimeMillis();
    }

    /**
     * Return the valid date of subscription. If subscription valid, return the date of expiration else if expired return 0
     *
     * @return
     */
    public long getDateSubscriptionValid() {
        if (isDateSubscriptionValid()) {
            return subscriptionExpirationDate;
        }
        return 0;
    }

    /**
     * Reset field avatar date upload & validation
     */
    public void removeAvatar() {
        avatarPresent = false; // TODO to remove
    }

    public void setNewAvatar() {
        avatarPresent = true; // TODO to remove
    }

    public boolean isFunbridge() {
        return ID == Constantes.PLAYER_FUNBRIDGE_ID;
    }

    public boolean isArgine() {
        return ID == Constantes.PLAYER_ARGINE_ID;
    }

    public int getCredentialsInitMask() {
        return credentialsInitMask;
    }

    public void setCredentialsInitMask(int credentialsInitMask) {
        this.credentialsInitMask = credentialsInitMask;
    }

    /**
     * Check if credentialInit mask included this flag
     * @param flag
     * @return
     */
    public boolean hasCredentialsInitFlag(int flag) {
        return ((credentialsInitMask & flag) == flag);
    }

    /**
     * Modify the value of credentialInit mask by adding this flag
     * @param flag
     */
    public void addCredentialsInitFlag(int flag) {
        credentialsInitMask = credentialsInitMask | flag;
    }

    /**
     * Modify the value of credentialInit mask by removing this flag
     * @param flag
     */
    public void removeCredentialsInitFlag(int flag) {
        credentialsInitMask = credentialsInitMask & (~flag);
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getLastConnectionDate() {
        return lastConnectionDate;
    }

    public void setLastConnectionDate(long lastConnectionDate) {
        this.lastConnectionDate = lastConnectionDate;
    }

    public int getNbPlayedDeals() {
        return nbPlayedDeals;
    }

    public void setNbPlayedDeals(int nbPlayedDeals) {
        this.nbPlayedDeals = nbPlayedDeals;
    }

    public long getSubscriptionExpirationDate() {
        return subscriptionExpirationDate;
    }

    public void setSubscriptionExpirationDate(long subscriptionExpirationDate) {
        this.subscriptionExpirationDate = subscriptionExpirationDate;
    }

    public int getFriendFlag() {
        return friendFlag;
    }

    public void setFriendFlag(int notificationFlag) {
        this.friendFlag = notificationFlag;
    }

    /**
     * Check if friend flag included this flag
     * @param flag
     * @return
     */
    public boolean hasFriendFlag(int flag) {
        return ((friendFlag & flag) == flag);
    }

    /**
     * Modify the value of friendFlag by adding this flag
     * @param flag
     */
    public void addFriendFlag(int flag) {
        friendFlag = friendFlag | flag;
    }

    /**
     * Modify the value of friendFlag by removing this flag
     * @param flag
     */
    public void removeFriendFlag(int flag) {
        friendFlag = friendFlag & (~flag);
    }

    @Override
    public String toString() {
        return "Player{" +
                "ID=" + ID +
                ", nickname='" + nickname + '\'' +
                ", password='" + password + '\'' +
                ", cert='" + cert + '\'' +
                ", type=" + type +
                ", mail='" + mail + '\'' +
                ", lang='" + lang + '\'' +
                ", displayLang='" + displayLang + '\'' +
                ", creationDate=" + creationDate +
                ", lastConnectionDate=" + lastConnectionDate +
                ", creditAmount=" + creditAmount +
                ", dateLastCreditUpdate=" + dateLastCreditUpdate +
                ", nbPlayedDeals=" + nbPlayedDeals +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", displayCountryCode='" + displayCountryCode + '\'' +
                ", town='" + town + '\'' +
                ", description='" + description + '\'' +
                ", birthday=" + birthday +
                ", avatarPresent=" + avatarPresent +
                ", settings='" + settings + '\'' +
                ", conventionProfile=" + conventionProfile +
                ", sex=" + sex +
                ", subscriptionExpirationDate=" + subscriptionExpirationDate +
                ", credentialsInitMask=" + credentialsInitMask +
                ", trial=" + trial +
                ", friendFlag=" + friendFlag +
                ", dayCreditAmount" + dayCreditAmount +
                '}';
    }
}

