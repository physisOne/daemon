package one.physis.daemon.services.projects.football;

import one.physis.daemon.data.repositories.AddressRepository;
import one.physis.daemon.data.repositories.MintRepository;
import one.physis.daemon.data.repositories.NftRepository;
import one.physis.daemon.services.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FootballWalletService extends WalletService {

   private final static String project = "football";

   public FootballWalletService(@Value("${projects." + project + ".name}") String name,
                            @Value("${projects." + project + ".port}") int port,
                            AddressRepository addressRepository,
                            MintRepository mintRepository,
                            NftRepository nftRepository,
                            RestTemplate restTemplate) {
      super(name, addressRepository, mintRepository, nftRepository, restTemplate, port);
   }

   @Override
   protected Logger getLogger() {
      return LoggerFactory.getLogger(project);
   }
}
