package one.physis.daemon.services.projects.grimverse;

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
public class GrimVerseWalletService extends WalletService {

   private final static String project = "grimverse";

   public GrimVerseWalletService(@Value("${projects." + project + ".name}") String name,
                                 @Value("${projects." + project + ".sendPort}") int sendPort,
                                 @Value("${projects." + project + ".receivePort}") int receivePort,
                                 AddressRepository addressRepository,
                                 MintRepository mintRepository,
                                 NftRepository nftRepository,
                                 RestTemplate restTemplate) {
      super(name, addressRepository, mintRepository, nftRepository, restTemplate, sendPort, receivePort);
   }

   @Override
   protected Logger getLogger() {
      return LoggerFactory.getLogger(project);
   }
}
