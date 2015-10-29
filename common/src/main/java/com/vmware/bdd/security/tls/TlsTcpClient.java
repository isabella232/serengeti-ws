/***************************************************************************
 * Copyright (c) 2013-2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.security.tls;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;


/**
 * Sample TCP client to demonstrate the right way to establish a secure
 * connection with a remote server. SSLSockets do not do hostname verification,
 * hence the TCP client uses an SSLSocketFactory with hostname verification
 * enabled.
 */
public class TlsTcpClient {
   private boolean pspCompliant = false;

   private SimpleServerTrustManager trustManager;

   public TrustManager getTrustManager() {
      return trustManager;
   }

   public boolean isPspCompliant() {
      return pspCompliant;
   }

   public void setPspCompliant(boolean pspCompliant) {
      this.pspCompliant = pspCompliant;
   }

   public void setTrustManager(SimpleServerTrustManager trustManager) {
      this.trustManager = trustManager;
   }


   /**
    * @param host
    * @param port
    */
   public void checkCertificateFirstly(String host, int port, final boolean forceTrustCert) {
      PspConfiguration config = new PspConfiguration();
      SSLSocket secureSocket = null;
      SSLSocketFactory socketFactory = null;

      trustManager.setTrustCertCallBack(new TrustCertCallBack() {
         @Override
         public boolean doTrustOnFirstUse(X509Certificate x509certificate) {
            return forceTrustCert;
         }
      });

      /**
       * Obtain a default implementation of client configuration
       */
      try {

         /**
          *  Initialize our own trust manager
          */
         TrustManager[] trustManagers = new TrustManager[]{trustManager};
         /**
          * Instantiate a context that implements the family of TLS protocols
          */
         SSLContext customSSLContext = SSLContext.getInstance(config.getSSLContextAlgorithm());
         /**
          * Initialize SSL context. Default instances of KeyManager and
          * SecureRandom are used.
          */
         customSSLContext.init(null, trustManagers, null);

         /**
          * Use the SSLSocketFactory generated by the SSLContext and wrap it to
          * enable custom cipher suites and protocols
          */
         socketFactory = customSSLContext.getSocketFactory();

      } catch (NoSuchAlgorithmException e) {
         throw new TlsInitException("SSLContext_INIT_ERR", e);
      } catch (KeyManagementException e) {
         throw new TlsInitException("SSLContext_INIT_ERR", e);
      }

      try {
         secureSocket = (SSLSocket) socketFactory.createSocket();

         InetSocketAddress address = new InetSocketAddress(host, port);

         if(isPspCompliant()) {
            /**
             * Build connection configuration and pass to socket
             */
            SSLParameters params = new SSLParameters();
            params.setCipherSuites(config.getSupportedCipherSuites());
            params.setProtocols(config.getSupportedProtocols());
//          params.setEndpointIdentificationAlgorithm(config.getEndpointIdentificationAlgorithm());
            secureSocket.setSSLParameters(params);
         }

         /**
          * Set socket options
          */
         secureSocket.setSoTimeout(10000);
         secureSocket.connect(address, 10000);
         secureSocket.startHandshake();

      } catch (ConnectException ex) {
         throw new TlsConnectionException(ex, ex.getMessage(), true);
      }
      catch (IOException i) {
         UntrustedCertificateException uce = TlsHelper.findRootCause(i, UntrustedCertificateException.class);
         if (uce != null) {
            throw uce;
         }

         TlsInitException tlse = TlsHelper.findRootCause(i, TlsInitException.class);
         if(tlse != null) {
            throw tlse;
         }

         throw new TlsConnectionException(i, i.getMessage(), false);
      } finally {
         try {
            if (secureSocket != null) {
               secureSocket.close();
            }
         } catch (IOException e) {
            //no way to handle it
         }
      }

   }

}