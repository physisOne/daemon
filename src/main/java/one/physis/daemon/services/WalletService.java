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

   private final static String WALLET_ID = "wallet";

   private final RestTemplate restTemplate;
   private final AddressRepository addressRepository;
   private final MintRepository mintRepository;
   private final NftRepository nftRepository;

   private final String name;
   private final String url;
   private final int port;

   private Date lastRestartDate;

   public WalletService(String name,
                        AddressRepository addressRepository,
                        MintRepository mintRepository,
                        NftRepository nftRepository,
                        RestTemplate restTemplate,
                        int port) {
      this.restTemplate = restTemplate;
      this.addressRepository = addressRepository;
      this.mintRepository = mintRepository;
      this.nftRepository = nftRepository;
      this.lastRestartDate = new Date();
      this.port = port;
      this.url = "http://127.0.0.1:" + port + "/";
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

      if(!isRunning()) {
         startWallet();
         try {
            Thread.sleep(1000 * 60 * 3);
         } catch (Exception ignored) {

         }
      }
   }

   private void startWallet() {
      getLogger().info("Starting wallet");
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);


      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add("seedKey", WALLET_ID);
      map.add("wallet-id", WALLET_ID);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

      ResponseEntity<Response> response = restTemplate.postForEntity(url + "start", request, Response.class );
      if(response.getStatusCode() != HttpStatus.OK) {
         getLogger().error("Could not start wallet  {}", response);
      } else {
         if (response.getBody() != null && response.getBody().isSuccess()) {
            getLogger().info("Wallet started");
         }
         else {
            getLogger().error("Could not start wallet {}", response.getBody().getMessage());
         }
      }
   }

   private boolean isRunning() {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("X-Wallet-Id", WALLET_ID);

      try {
         ResponseEntity<StatusResponse> response = restTemplate.exchange(url + "wallet/status", HttpMethod.GET, new HttpEntity<>(headers),
                 StatusResponse.class);

         if (response.getStatusCode() == HttpStatus.OK) {
            StatusResponse status = response.getBody();
            return status != null && status.getStatusCode() != null && status.getStatusCode() == 3;
         }
      } catch (Exception ex) {
         getLogger().error("unable to find out if wallet is running", ex);
      }

      return false;
   }

   public Addresses getAddresses () {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("X-Wallet-Id", WALLET_ID);

      try {
         ResponseEntity<Addresses> response = restTemplate.exchange(this.url + "wallet/addresses", HttpMethod.GET, new HttpEntity<>(headers),
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

   public Integer checkHtrBalance() {
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("X-Wallet-Id", WALLET_ID);

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
      headers.add("X-Wallet-Id", WALLET_ID);

      try {
         ResponseEntity<Balance> response = restTemplate.exchange(this.url + "wallet/utxo-filter?filter_address=" + address, HttpMethod.GET, new HttpEntity<>(headers),
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
      headers.add("X-Wallet-Id", WALLET_ID);
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
         ResponseEntity<SendResponse> response = restTemplate.postForEntity(this.url + "wallet/send-tx", request, SendResponse.class);

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
      headers.add("X-Wallet-Id", WALLET_ID);
      headers.setContentType(MediaType.APPLICATION_JSON);

      SendTransaction transaction = new SendTransaction();
      Output output = new Output();
      output.setAddress(address);
      output.setValue(amount);
      transaction.getOutputs().add(output);

      transaction.setInputs(new ArrayList<>());
      for(Utxo utxo : utxos) {
         Input input = new Input();
         input.setHash(utxo.getTx_id());
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
         ResponseEntity<SendResponse> response = restTemplate.postForEntity(this.url + "wallet/send-tx", request, SendResponse.class);

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
      headers.add("X-Wallet-Id", WALLET_ID);
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
         ResponseEntity<SendResponse> response = restTemplate.postForEntity(this.url + "/wallet/simple-send-tx", request, SendResponse.class);

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
      headers.add("X-Wallet-Id", WALLET_ID);
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add("name", name);
      map.add("symbol", symbol);
      map.add("amount", "1");
      map.add("data", data);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

      try {
         ResponseEntity<CreateNftResponse> response = restTemplate.postForEntity(this.url + "wallet/create-nft", request, CreateNftResponse.class);
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
