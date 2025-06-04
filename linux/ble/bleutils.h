#pragma once

#include <QObject>
#include <QByteArray>

class BLEUtils : public QObject
{
    Q_OBJECT
public:
    explicit BLEUtils(QObject *parent = nullptr);

    /**
     * @brief Verifies if the provided Bluetooth address is an RPA that matches the given Identity Resolving Key (IRK)
     * @param address The Bluetooth address to verify
     * @param irk The Identity Resolving Key to use for verification
     * @return true if the address is verified as an RPA matching the IRK
     */
    static bool verifyRPA(const QString &address, const QByteArray &irk);

    /**
     * @brief Checks if the given IRK and RPA are valid
     * @param irk The Identity Resolving Key
     * @param rpa The Resolvable Private Address
     * @return true if the RPA is valid for the given IRK
     */
    Q_INVOKABLE static bool isValidIrkRpa(const QByteArray &irk, const QString &rpa);

    /**
     * @brief Decrypts the last 16 bytes of the input data using the provided key with AES-128 ECB
     * @param data The input data containing at least 16 bytes
     * @param key The 16-byte key for decryption
     * @return The decrypted 16 bytes, or an empty QByteArray on failure
     */
    static QByteArray decryptLastBytes(const QByteArray &data, const QByteArray &key);

private:
    /**
     * @brief Performs E function (AES-128) as specified in Bluetooth Core Specification
     * @param key The key for encryption
     * @param data The data to encrypt
     * @return The encrypted data
     */
    static QByteArray e(const QByteArray &key, const QByteArray &data);

    /**
     * @brief Performs the ah function as specified in Bluetooth Core Specification
     * @param k The IRK key
     * @param r The random part of the address
     * @return The hash part of the address
     */
    static QByteArray ah(const QByteArray &k, const QByteArray &r);
};