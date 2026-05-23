package com.susterku.nurse.config;

import com.susterku.nurse.entity.NurseEntity;
import com.susterku.nurse.repository.NurseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final NurseRepository nurseRepository;

    @Override
    public void run(String... args) {
        if (nurseRepository.count() > 0) return;

        log.info("Seeding nurse data...");

        List<NurseEntity> nurses = List.of(
            NurseEntity.builder()
                .name("Sari Dewi")
                .avatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Sari&backgroundColor=b6e3f4")
                .age(32).experience(7).rating(4.9).totalReviews(124)
                .specializations(List.of("Bayi Baru Lahir", "Prematur", "ASI Eksklusif"))
                .location("Jakarta Selatan").pricePerDay(new BigDecimal("350000"))
                .isAvailable(true)
                .bio("Suster berpengalaman dengan sertifikasi perawatan bayi dari RSIA Bunda. Spesialis bayi baru lahir dan pendampingan ibu menyusui.")
                .build(),

            NurseEntity.builder()
                .name("Yuli Rahayu")
                .avatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Yuli&backgroundColor=ffdfbf")
                .age(28).experience(4).rating(4.7).totalReviews(86)
                .specializations(List.of("Bayi 0-12 Bulan", "Pijat Bayi", "Baby Swim"))
                .location("Jakarta Barat").pricePerDay(new BigDecimal("300000"))
                .isAvailable(true)
                .bio("Suster muda energik dengan keahlian pijat bayi bersertifikat. Berpengalaman menangani bayi usia 0-12 bulan.")
                .build(),

            NurseEntity.builder()
                .name("Fitri Handayani")
                .avatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Fitri&backgroundColor=c0aede")
                .age(45).experience(15).rating(5.0).totalReviews(210)
                .specializations(List.of("Senior Nanny", "Bayi Kembar", "Toddler 1-3 Tahun"))
                .location("Jakarta Pusat").pricePerDay(new BigDecimal("450000"))
                .isAvailable(false)
                .bio("Suster senior dengan pengalaman 15 tahun. Ahli menangani bayi kembar dan kebutuhan khusus. Dipercaya lebih dari 200 keluarga.")
                .build(),

            NurseEntity.builder()
                .name("Nurul Aini")
                .avatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Nurul&backgroundColor=d1f0c1")
                .age(35).experience(8).rating(4.8).totalReviews(97)
                .specializations(List.of("Bayi Sakit", "MPASI", "Sleep Training"))
                .location("Tangerang Selatan").pricePerDay(new BigDecimal("380000"))
                .isAvailable(true)
                .bio("Memiliki latar belakang keperawatan dari STIKes Jakarta. Spesialis pendampingan MPASI dan sleep training untuk bayi.")
                .build(),

            NurseEntity.builder()
                .name("Lestari Wahyu")
                .avatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Lestari&backgroundColor=ffd5dc")
                .age(30).experience(5).rating(4.6).totalReviews(63)
                .specializations(List.of("Bayi Baru Lahir", "Ibu Pemula", "Breastfeeding"))
                .location("Bekasi").pricePerDay(new BigDecimal("280000"))
                .isAvailable(true)
                .bio("Suster ramah dan komunikatif. Senang mendampingi ibu baru dalam merawat buah hati mereka dengan penuh kasih sayang.")
                .build(),

            NurseEntity.builder()
                .name("Retno Kumalasari")
                .avatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Retno&backgroundColor=ffeaa7")
                .age(38).experience(10).rating(4.9).totalReviews(158)
                .specializations(List.of("Bayi Prematur", "Medis Khusus", "NICU Care"))
                .location("Depok").pricePerDay(new BigDecimal("500000"))
                .isAvailable(true)
                .bio("Mantan perawat NICU RS Cipto Mangunkusumo selama 10 tahun. Ahli penanganan bayi prematur dan kondisi medis khusus.")
                .build()
        );

        nurseRepository.saveAll(nurses);
        log.info("Seeded {} nurses.", nurses.size());
    }
}
