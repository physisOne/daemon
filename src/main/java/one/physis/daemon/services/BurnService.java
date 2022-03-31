package one.physis.daemon.services;

import com.google.gson.Gson;
import one.physis.daemon.data.entities.Nft;
import one.physis.daemon.data.entities.Project;
import one.physis.daemon.data.repositories.NftRepository;
import one.physis.daemon.data.repositories.ProjectRepository;
import one.physis.daemon.services.nft.Attribute;
import one.physis.daemon.services.projects.grimverse.GrimverseWalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BurnService {

   Logger logger = LoggerFactory.getLogger(BurnService.class);

   private final NftRepository nftRepository;
   private final WalletService walletService;
   private final RetryTemplate retryTemplate;

   private final int PROJECT_ID = 2;
   private final int NFT_COUNT_PER_ONCE = 10;
   private final int COUNT = 2000;

   private final List<Integer> SPECIALS = Arrays.asList(new Integer[]{ 666, 1666 });

   private final Gson gson;

   public BurnService(NftRepository nftRepository,
                      GrimverseWalletService walletService, //TODO THIS!!!
                      RetryTemplate retryTemplate) {
      this.nftRepository = nftRepository;
      this.walletService = walletService;
      this.retryTemplate = retryTemplate;

      this.gson = new Gson();
   }

   //@PostConstruct
   public void printNewCount() {
      List<Nft> nfts = nftRepository.findByProjectIdAndBurnedIsFalse(PROJECT_ID);

      Map<String, Map<String, Integer>> rarityMap = new HashMap<>();

      for(Nft nft : nfts) {
         String json = nft.getAttributes();
         one.physis.daemon.services.nft.Nft n = this.gson.fromJson(json, one.physis.daemon.services.nft.Nft.class);

         for(Attribute attribute : n.getAttributes()) {
            if(!rarityMap.containsKey(attribute.getType())){
               Map<String, Integer> valueMap = new HashMap<>();
               valueMap.put(attribute.getValue(), 1);
               rarityMap.put(attribute.getType(), valueMap);
            } else {
               Map<String, Integer> valueMap = rarityMap.get(attribute.getType());
               if(!valueMap.containsKey(attribute.getValue())) {
                  valueMap.put(attribute.getValue(), 1);
               } else {
                  valueMap.put(attribute.getValue(), valueMap.get(attribute.getValue()) + 1);
               }
            }
         }
      }

      String rarity = gson.toJson(rarityMap);
      logger.info(rarity);
   }

   //@PostConstruct
   public void burn(){
      int p = COUNT / NFT_COUNT_PER_ONCE;
      for(int i = 0; i < p; i++) {
         logger.info("Burning nfts " + i);
         List<Nft> nfts = filterOutSpecials(nftRepository.findNotTaken(NFT_COUNT_PER_ONCE, PROJECT_ID));
         List<String> tokens = nfts.stream().map(it -> it.getToken()).collect(Collectors.toList());
         String hash = walletService.sendTokens("HDeadDeadDeadDeadDeadDeadDeagTPgmn", tokens);
         if(hash != null) {
            nfts.forEach(it -> {
               it.setBurnTransaction(hash);
               it.setTaken(true);
               it.setBurned(true);
            });
            try {
               retryTemplate.execute(context -> {
                  nftRepository.saveAll(nfts);
                  return null;
               });
            } catch (Exception ex) {
               logger.error("could not save nfts", ex);
               logger.error(hash);
               for (Nft s : nfts) {
                  logger.error("NFT " + s.getId() + " not saved");
               }
            }
         } else {
            logger.error("Could not burn nfts!");
         }
      }
   }

   private List<Nft> filterOutSpecials(List<Nft> nfts) {
      List<Nft> result = new ArrayList<>();
      for(Nft nft : nfts) {
         Integer number = nft.getNumber();
         if(!SPECIALS.contains(number)) {
            result.add(nft);
         } else {
            logger.info("Special " + number + " was filtered out");
         }
      }
      return result;
   }
}
