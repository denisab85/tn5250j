package org.tn5250j.framework.transport.SSL;

/*
 * @(#)X509CertificateTrustManager.java
 *
 * Copyright:    Copyright (c) 2001
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 *
 */

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;

import org.tn5250j.interfaces.HeadlessSessionUiHooks;
import org.tn5250j.interfaces.SessionUiHooks;

/**
 * This class is used to trust certificates exchanged during an SSL socket
 * handshake.  It allows the user to accept the certificate so that connections
 * can be made without requiring the server to have a certificate signed by a
 * CA (Verisign, Thawte, etc.).
 *
 * @author Stephen M. Kennedy <skennedy@tenthpowertech.com>
 * @deprecated. no longer used.
 */
public class X509CertificateTrustManager implements X509TrustManager {

    KeyStore ks = null;
    final TrustManager[] trustManagers;
    private SessionUiHooks trustHooks = HeadlessSessionUiHooks.INSTANCE;
    //X509TrustManager trustManager = null;

    public X509CertificateTrustManager(TrustManager[] managers, KeyStore keyStore) {
        trustManagers = managers;
        ks = keyStore;
    }

    /**
     * Replaces the default reject-untrusted behavior. With no hooks installed,
     * untrusted certificates are rejected and no dialog is shown.
     *
     * @param hooks UI callback, or null to reject untrusted certificates
     */
    public void setTrustHooks(SessionUiHooks hooks) {
        trustHooks = hooks == null ? HeadlessSessionUiHooks.INSTANCE : hooks;
    }

    public void checkClientTrusted(X509Certificate[] chain, String type) throws CertificateException {
        throw new SecurityException("checkClientTrusted unsupported");
    }


    /**
     * Checks the server certificate.  If it isn't trusted by the trust manager
     * passed to the constructor, then the user will be prompted to accept the
     * certificate.
     */
    public void checkServerTrusted(X509Certificate[] chain, String type)
            throws CertificateException {
        try {
            for (TrustManager trustManager : trustManagers) {
                if (trustManager instanceof X509TrustManager)
                    ((X509TrustManager) trustManager).checkServerTrusted(chain, type);
            }
            return;
        } catch (CertificateException ce) {
            X509Certificate cert = chain[0];
            String certInfo = "Version: " + cert.getVersion() + "\n";
            certInfo = certInfo.concat("Serial Number: " + cert.getSerialNumber() + "\n");
            certInfo = certInfo.concat("Signature Algorithm: " + cert.getSigAlgName() + "\n");
            certInfo = certInfo.concat("Issuer: " + cert.getIssuerDN().getName() + "\n");
            certInfo = certInfo.concat("Valid From: " + cert.getNotBefore() + "\n");
            certInfo = certInfo.concat("Valid To: " + cert.getNotAfter() + "\n");
            certInfo = certInfo.concat("Subject DN: " + cert.getSubjectDN().getName() + "\n");
            certInfo = certInfo.concat("Public Key: " + cert.getPublicKey().getFormat() + "\n");

            if (!trustHooks.acceptUntrustedCertificate(certInfo)) {
                throw new java.security.cert.CertificateException("Certificate Not Accepted");
            }
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        ArrayList<X509Certificate> list = new ArrayList<>(10);
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager)
                list.addAll(Arrays.asList(((X509TrustManager) trustManager).getAcceptedIssuers()));
        }
        X509Certificate[] acceptedIssuers = new X509Certificate[list.size()];
        acceptedIssuers = list.toArray(acceptedIssuers);
        return acceptedIssuers;
    }
}
