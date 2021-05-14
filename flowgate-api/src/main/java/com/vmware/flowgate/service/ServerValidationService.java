/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.service;

import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.vmware.flowgate.auth.AuthVcUser;
import com.vmware.flowgate.auth.InfobloxAuth;
import com.vmware.flowgate.auth.NlyteAuth;
import com.vmware.flowgate.auth.OpenManageAuth;
import com.vmware.flowgate.auth.PowerIQAuth;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.common.model.SDDCSoftwareConfig;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.verifycert.VRopsAuth;
import com.vmware.ops.api.client.exceptions.AuthException;
import com.vmware.vim.binding.vim.fault.InvalidLocale;
import com.vmware.vim.binding.vim.fault.InvalidLogin;
import com.vmware.vim.vmomi.client.exception.ConnectionException;
import com.vmware.vim.vmomi.client.exception.SslException;

@Component
public class ServerValidationService {
   public void validVCServer(SDDCSoftwareConfig server) {
      AuthVcUser authVcUser = getVcAuth(server);
      try {

         authVcUser.authenticateUser(server.getUserName(), server.getPassword());

      }catch(SslException | SSLException e) {
         throw new WormholeRequestException(HttpStatus.BAD_REQUEST,"Certificate verification error",e.getCause());
      }catch(AuthException | InvalidLogin e) {
         throw new WormholeRequestException(HttpStatus.BAD_REQUEST,"Invalid user name or password",e.getCause());
      }catch(ConnectionException | URISyntaxException | InvalidLocale e) {
         throw new WormholeRequestException(HttpStatus.BAD_REQUEST,"Failed to connect to server",e.getCause());
      }
   }

   public AuthVcUser getVcAuth(SDDCSoftwareConfig server) {
      return new AuthVcUser(server.getServerURL(),443, !server.isVerifyCert());
   }

   public void validateVROServer(SDDCSoftwareConfig server) {
      try {
         VRopsAuth ss = createVRopsAuth(server);
         ss.getClient().apiVersionsClient().getCurrentVersion();
      } catch (AuthException authException) {
         throw new WormholeRequestException(HttpStatus.BAD_REQUEST,
               "Invalid user name or password.", authException.getCause());
      } catch (Exception exception) {
         if (exception.getCause() instanceof ConnectException) {
            throw new WormholeRequestException(HttpStatus.BAD_REQUEST,
                  "Failed to connect to server: " + server.getServerURL(), exception.getCause());
         } else if (exception.getCause() instanceof SSLException) {
            throw new WormholeRequestException(HttpStatus.BAD_REQUEST,
                  "Certificate verification error", exception.getCause());
         }
         throw new WormholeRequestException("Internal error",exception.getCause());
      }
   }

   public VRopsAuth createVRopsAuth(SDDCSoftwareConfig server) {
      return new VRopsAuth(server);
   }

   public void validateFacilityServer(FacilitySoftwareConfig config)
         throws WormholeRequestException {
      try {
         switch (config.getType()) {
         case Nlyte:
            NlyteAuth nlyteAuth = createNlyteAuth();
            if (!nlyteAuth.auth(config)) {
               throw new WormholeRequestException(HttpStatus.BAD_REQUEST,
                     "Invalid user name or password.", null);
            }
            break;
         case PowerIQ:
            PowerIQAuth powerIQAuth = createPowerIQAuth();
            if (!powerIQAuth.auth(config)) {
               throw new WormholeRequestException(HttpStatus.BAD_REQUEST,
                     "Invalid user name or password.", null);
            }
            break;
         case InfoBlox:
            InfobloxAuth infobloxAuth = createInfobloxAuth();
            if (!infobloxAuth.auth(config)) {
               throw new WormholeRequestException(HttpStatus.BAD_REQUEST,
                     "Invalid user name or password.", null);
            }
            break;
         case OpenManage:
            OpenManageAuth openmanageAuth = createOpenManageAuth();
            if (!openmanageAuth.auth(config)) {
               throw new WormholeRequestException(HttpStatus.BAD_REQUEST,
                     "Invalid user name or password.", null);
            }
            break;
         case Device42:
            break;
         case OtherDCIM:
            break;
         case Labsdb:
            break;
         case OtherCMDB:
            break;
         default:
            throw WormholeRequestException.InvalidFiled("type", config.getType().toString());
         }
      } catch (ResourceAccessException e) {
         if (e.getCause() instanceof SSLException) {
            throw new WormholeRequestException(HttpStatus.BAD_REQUEST,
                  "Certificate verification error", e.getCause(),
                  WormholeRequestException.InvalidSSLCertificateCode);
         } else if (e.getCause() instanceof UnknownHostException) {
            throw new WormholeRequestException(HttpStatus.BAD_REQUEST, "Unknown Host.Please check your server IP.", e.getCause(),
                  WormholeRequestException.UnknownHostCode);
         } else if (e.getCause() instanceof ConnectException) {
            throw new WormholeRequestException(HttpStatus.BAD_REQUEST, "Connect failed.", e.getCause());
         }
         throw new WormholeRequestException(HttpStatus.BAD_REQUEST, e.getMessage(), e.getCause());
      } catch (UnknownHostException e) {
         throw new WormholeRequestException(HttpStatus.BAD_REQUEST, "Unknown Host.Please check your server IP.", e.getCause(),
               WormholeRequestException.UnknownHostCode);
      } catch (SSLException | KeyManagementException | CertificateException
            | NoSuchAlgorithmException | KeyStoreException e) {
         throw new WormholeRequestException(HttpStatus.BAD_REQUEST,
               "Certificate verification error", e.getCause(),
               WormholeRequestException.InvalidSSLCertificateCode);
      } catch (HttpClientErrorException e) {
         if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
            throw new WormholeRequestException(HttpStatus.BAD_REQUEST,
                  "Invalid user name or password.", e.getCause());
         } else if (HttpStatus.FORBIDDEN.equals(e.getStatusCode())) {
            throw new WormholeRequestException(HttpStatus.FORBIDDEN, "403 Forbidden", e.getCause());
         } else if(HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
            throw new WormholeRequestException(HttpStatus.BAD_REQUEST, "Unknown Host.Please check your server IP.", e.getCause(),
                  WormholeRequestException.UnknownHostCode);
         }
         throw new WormholeRequestException(HttpStatus.BAD_REQUEST, e.getMessage(), e.getCause());
      }
   }

   public OpenManageAuth createOpenManageAuth() {
      return new OpenManageAuth();
   }

   public NlyteAuth createNlyteAuth() {
      return new NlyteAuth();
   }

   public PowerIQAuth createPowerIQAuth() {
      return new PowerIQAuth();
   }

   public InfobloxAuth createInfobloxAuth() {
      return new InfobloxAuth();
   }
}
