package one.physis.daemon.services.projects.football;


import one.physis.daemon.data.entities.Nft;
import one.physis.daemon.data.repositories.MintRepository;
import one.physis.daemon.data.repositories.NftRepository;
import one.physis.daemon.data.repositories.ProjectRepository;
import one.physis.daemon.services.DaemonService;
import one.physis.daemon.services.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FootballDaemonService extends DaemonService<FootballWalletService> {

   private final static String project = "football";

   public FootballDaemonService(@Value("${projects." + project + ".name}") String name,
                                 @Value("${projects." + project + ".id}") int projectId,
                                 @Value("${projects." + project + ".customerAddress}") String customerAddress,
                                 @Value("${projects." + project + ".percentage}") int percentage,
                                 @Value("${projects." + project + ".enabled}") boolean enabled,
                                 MintRepository mintRepository,
                                 ProjectRepository projectRepository,
                                 FootballWalletService walletService, //BEWARE OF THIS!
                                 NftRepository nftRepository,
                                 RetryTemplate retryTemplate,
                                 MailService mailService) {
      super(name, projectId, mintRepository, projectRepository, walletService, nftRepository, retryTemplate, mailService, customerAddress, percentage, enabled);
   }

   @Override
   protected Logger getLogger() {
      return LoggerFactory.getLogger(project);
   }

   @PostConstruct
   public void init() {
      String address = "HHomhwKWRJf7fgCPe3ciRGooDCNZmgEo49";
      sendMultipleNfts(address, Arrays.asList(1, 159, 228, 262, 283, 321, 352, 378, 472, 535, 536, 649, 731, 763, 770), true);
      sendMultipleNfts(address, Arrays.asList(1060, 1305, 1490, 1968, 2161, 2286, 2289, 2899, 2927, 3078, 3190, 3270, 3404, 3803), true);
      sendMultipleNfts(address, Arrays.asList(4562, 4586, 4840, 5289, 5529, 5770, 5787, 5918, 6663, 7065, 7118), true);
      sendMultipleNfts(address, Arrays.asList(7412, 7497, 8151, 8177, 9000, 9060, 9062, 9371, 9383, 9989), true);
   }
}
