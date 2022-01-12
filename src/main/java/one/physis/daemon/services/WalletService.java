package one.physis.daemon.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import one.physis.daemon.data.entities.Address;
import one.physis.daemon.data.repositories.AddressRepository;
import one.physis.daemon.data.repositories.MintRepository;
import one.physis.daemon.data.repositories.NftRepository;
import one.physis.daemon.services.dto.*;
import org.slf4j.Logger;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class WalletService {

   protected abstract Logger getLogger();

   private final static String SEND_ID = "send";
   private final static String RECEIVE_ID = "receive";

   private final RestTemplate restTemplate;
   private final AddressRepository addressRepository;
   private final MintRepository mintRepository;
   private final NftRepository nftRepository;

   private final String name;
   private final String sendUrl;
   private final String receiveUrl;
   private final int sendPort;
   private final int receivePort;

   private Date lastRestartDate;

   public WalletService(String name,
                        AddressRepository addressRepository,
                        MintRepository mintRepository,
                        NftRepository nftRepository,
                        RestTemplate restTemplate,
                        int sendPort, int receivePort) {
      this.restTemplate = restTemplate;
      this.addressRepository = addressRepository;
      this.mintRepository = mintRepository;
      this.nftRepository = nftRepository;
      this.lastRestartDate = new Date();
      this.sendPort = sendPort;
      this.receivePort = receivePort;
      this.sendUrl = "http://127.0.0.1:" + sendPort + "/";
      this.receiveUrl = "http://127.0.0.1:" + receivePort + "/";
      this.name = name;
   }


   public void checkWallets() {
//      Date now = new Date();
//      long diff = now.getTime() - lastRestartDate.getTime();
//      long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
//      if(minutes >= 90) {
//         if(killWallets()) {
//            lastRestartDate = new Date();
//            return;
//         }
//      }

      if(!isRunning(true)) {
         startWallet(true);
         try {
            Thread.sleep(1000 * 60 * 3);
         } catch (Exception ignored) {

         }
      }

      if(!isRunning(false)) {
         startWallet(false);
         try {
            Thread.sleep(1000 * 60 * 3);
         } catch (Exception ignored) {

         }
      }
   }

   public boolean killWallets() {
      try {
         getLogger().info("Killing wallets!");
         Runtime rt = Runtime.getRuntime();
         String[] cmdReceive = {
                 "/bin/zsh",
                 "-c",
                 "kill -9 $(lsof -ti:" + this.receivePort + ")"
         };
         String[] cmdSend = {
                 "/bin/zsh",
                 "-c",
                 "kill -9 $(lsof -ti:" + this.sendPort + ")"
         };

         Process receiveProcess = rt.exec(cmdReceive);
         getLogger().info("Receive wallet killed!");
         Thread.sleep(1000);
         Process sendProcess = rt.exec(cmdSend);
         getLogger().info("Send wallet killed!");

         Thread.sleep(10000);

         while(!isRunning(true)) {
            startWallet(true);
            try {
               Thread.sleep(1000 * 60);
            } catch (Exception ignored) {

            }
         }
         getLogger().info("Wallet send initialized!");

         while(!isRunning(false)) {
            startWallet(false);
            try {
               Thread.sleep(1000 * 60);
            } catch (Exception ignored) {

            }
         }
         getLogger().info("Wallet receive initialized!");

         return true;
      } catch (Exception ex) {
         getLogger().error("Unable to kill wallet");
      }

      return false;
   }

   private void startWallet(boolean send) {
      getLogger().info("Starting wallets");
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      String id = send ? SEND_ID : RECEIVE_ID;
      String url = send ? this.sendUrl : this.receiveUrl;

      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add("seedKey", id);
      map.add("wallet-id", id);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

      ResponseEntity<Response> response = restTemplate.postForEntity(url + "start", request, Response.class );
      if(response.getStatusCode() != HttpStatus.OK) {
         getLogger().error("Could not start wallet " + id + " {}", response);
      } else {
         if (response.getBody() != null && response.getBody().isSuccess()) {
            getLogger().info("Wallet " + id + " started");
         }
         else {
            getLogger().error("Could not start wallet {}", response.getBody().getMessage());
         }
      }
   }

   private boolean isRunning(boolean send) {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("X-Wallet-Id", send ? SEND_ID : RECEIVE_ID);
      String url = send ? this.sendUrl : this.receiveUrl;

      try {
         ResponseEntity<StatusResponse> response = restTemplate.exchange(url + "wallet/status", HttpMethod.GET, new HttpEntity<>(headers),
                 StatusResponse.class);

         if (response.getStatusCode() == HttpStatus.OK) {
            StatusResponse status = response.getBody();
            return status != null && status.getStatusCode() != null && status.getStatusCode() == 3;
         }
      } catch (Exception ex) {
         getLogger().error("unable to find out if wallet is running, " + send, ex);
      }

      return false;
   }

   public Addresses getAddresses () {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("X-Wallet-Id", RECEIVE_ID);

      try {
         ResponseEntity<Addresses> response = restTemplate.exchange(this.receiveUrl + "wallet/addresses", HttpMethod.GET, new HttpEntity<>(headers),
                 Addresses.class);

         if (response.getStatusCode() == HttpStatus.OK) {
            Addresses addresses = response.getBody();
            return addresses;
         }
      } catch (Exception ex) {
         getLogger().error("Unable to get addresses", ex);
      }

      return null;
   }

   public void initAddresses() {
      Addresses addresses = getAddresses();

      if (addresses != null) {
         if (addresses != null && addresses.getAddresses() != null) {
            //addressRepository.deleteAll();
            for (String address : addresses.getAddresses()) {
               Address ad = new Address();
               ad.setAddress(address);
               addressRepository.save(ad);
            }
         }
      }
   }

   public Integer checkHtrBalance(boolean send) {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("X-Wallet-Id", send ? SEND_ID : RECEIVE_ID);
      String url = send ? this.sendUrl : this.receiveUrl;

      try {
         ResponseEntity<Balance> response = restTemplate.exchange(url + "wallet/balance", HttpMethod.GET, new HttpEntity<>(headers),
                 Balance.class);

         if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getAvailable();
         }
      } catch (Exception ex) {
         getLogger().error("Unable to get HTR balance", ex);
      }

      return null;
   }

   public Balance checkBalance(String address) {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("X-Wallet-Id", RECEIVE_ID);

      try {
         ResponseEntity<Balance> response = restTemplate.exchange(this.receiveUrl + "wallet/utxo-filter?filter_address=" + address, HttpMethod.GET, new HttpEntity<>(headers),
                 Balance.class);

         if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
         }
      } catch (Exception ex) {
         getLogger().error("Unable to get balance for address " + address, ex);
      }

      return null;
   }

   public String sendTokens(String address, List<String> tokens) {
      getLogger().info("Sending tokens to " + address);
      HttpHeaders headers = new HttpHeaders();
      headers.add("X-Wallet-Id", SEND_ID);
      headers.setContentType(MediaType.APPLICATION_JSON);

      SendTransaction transaction = new SendTransaction();
      for (String token : tokens) {
         getLogger().info("Adding token " + token);
         Output output = new Output();
         output.setAddress(address);
         output.setValue(1);
         output.setToken(token);
         transaction.getOutputs().add(output);
      }

      String json;
      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      try {
         json = ow.writeValueAsString(transaction);
      } catch (JsonProcessingException ex) {
         getLogger().error("Failed to create json for sendTokens to address " + address, ex);
         return null;
      }

      HttpEntity<String> request = new HttpEntity<>(json, headers);

      try {
         ResponseEntity<SendResponse> response = restTemplate.postForEntity(this.sendUrl + "wallet/send-tx", request, SendResponse.class);

         if (response.getBody() != null && response.getBody().isSuccess()) {
            getLogger().info("Successfully sent tokens to {}", address);
            return response.getBody().getHash();
         } else {
            getLogger().error("Unable to send to address " + address);
         }
      } catch (Exception ex) {
         getLogger().error("Unable to send to address " + address, ex);
      }

      return null;
   }

   public String sendHtrFromInputTransaction(String address, int amount, List<Utxo> utxos) {
      getLogger().info("Sending Htr to " + address);
      HttpHeaders headers = new HttpHeaders();
      headers.add("X-Wallet-Id", RECEIVE_ID);
      headers.setContentType(MediaType.APPLICATION_JSON);

      SendTransaction transaction = new SendTransaction();
      Output output = new Output();
      output.setAddress(address);
      output.setValue(amount);
      transaction.getOutputs().add(output);

      transaction.setInputs(new ArrayList<>());
      for(Utxo utxo : utxos) {
         Input input = new Input();
         input.setHash(utxo.getTxId());
         input.setIndex(utxo.getIndex());
         transaction.getInputs().add(input);
      }

      String json;
      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      try {
         json = ow.writeValueAsString(transaction);
      } catch (JsonProcessingException ex) {
         getLogger().error("Failed to create json for sendHtrFromInputTransaction to address " + address, ex);
         return null;
      }

      HttpEntity<String> request = new HttpEntity<>(json, headers);

      try {
         ResponseEntity<SendResponse> response = restTemplate.postForEntity(this.sendUrl + "wallet/send-tx", request, SendResponse.class);

         if (response.getBody() != null && response.getBody().isSuccess()) {
            getLogger().info("Successfully sent htr to {}", address);
            return response.getBody().getHash();
         } else {
            getLogger().error("Unable to send to address " + address);
         }
      } catch (Exception ex) {
         getLogger().error("Unable to send to address " + address, ex);
      }

      return null;
   }

   public String sendHtr(String address, int amount) {
      getLogger().info("Sending " + amount + " HTR to address " + address);
      HttpHeaders headers = new HttpHeaders();
      headers.add("X-Wallet-Id", SEND_ID);
      headers.setContentType(MediaType.APPLICATION_JSON);

      SimpleSendTransaction transaction = new SimpleSendTransaction(address, amount);

      String json;
      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      try {
         json = ow.writeValueAsString(transaction);
      } catch (JsonProcessingException ex) {
         getLogger().error("Failed to create json for sendHtr to address " + address, ex);
         return null;
      }

      HttpEntity<String> request = new HttpEntity<>(json, headers);

      try {
         ResponseEntity<SendResponse> response = restTemplate.postForEntity(this.sendUrl + "/wallet/simple-send-tx", request, SendResponse.class);

         if (response.getBody() != null && response.getBody().isSuccess()) {
            getLogger().info("Successfully sent tokens to {}", address);
            return response.getBody().getHash();
         } else {
            getLogger().error("Unable to send htr to address " + address);
         }
      } catch (Exception ex) {
         getLogger().error("Unable to send htr!", ex);
      }

      return null;
   }

   public String sendHtrFromReceive(String address, int amount) {
      getLogger().info("Sending " + amount + " HTR to address " + address);
      HttpHeaders headers = new HttpHeaders();
      headers.add("X-Wallet-Id", RECEIVE_ID);
      headers.setContentType(MediaType.APPLICATION_JSON);

      SimpleSendTransaction transaction = new SimpleSendTransaction(address, amount);

      String json;

      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      try {
         json = ow.writeValueAsString(transaction);
      } catch (JsonProcessingException ex) {
         getLogger().error("Failed to create json for sendHtrFromReceive to address " + address, ex);
         return null;
      }

      HttpEntity<String> request = new HttpEntity<>(json, headers);

      try {
         ResponseEntity<SendResponse> response = restTemplate.postForEntity(this.receiveUrl + "/wallet/simple-send-tx", request, SendResponse.class);

         if (response.getBody() != null && response.getBody().isSuccess()) {
            getLogger().info("Successfully sent tokens to {}", address);
            return response.getBody().getHash();
         } else {
            getLogger().error("Unable to send htr to address " + address);
         }
      } catch (Exception ex) {
         getLogger().error("Unable to send htr!", ex);
      }

      return null;
   }

   public String createNft(String name, String symbol, String data) {
      HttpHeaders headers = new HttpHeaders();
      headers.add("X-Wallet-Id", SEND_ID);
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add("name", name);
      map.add("symbol", symbol);
      map.add("amount", "1");
      map.add("data", data);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

      try {
         ResponseEntity<CreateNftResponse> response = restTemplate.postForEntity(this.sendUrl + "wallet/create-nft", request, CreateNftResponse.class);
         if (response.getStatusCode() != HttpStatus.OK) {
            getLogger().error("Could not create nft", response);
         } else {
            if (response.getBody() != null && response.getBody().isSuccess()) {
               getLogger().info("Nft created successfully");
               return response.getBody().getHash();
            } else {
               getLogger().error("Could not create nft {}", response.getBody().getMessage());
            }
         }
      } catch (Exception ex) {
         getLogger().error("Could not create nft", ex);
      }
      return null;
   }
}
