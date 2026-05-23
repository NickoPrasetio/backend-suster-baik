package com.tukangku.worker.config;

import com.tukangku.worker.entity.WorkerEntity;
import com.tukangku.worker.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final WorkerRepository workerRepository;

    @Value("${minio.public-url}")
    private String minioPublicUrl;

    @Override
    public void run(String... args) {
        if (workerRepository.count() > 0) return;

        log.info("Seeding worker data...");

        String bucket = minioPublicUrl + "/worker-photos/seed-worker-";
        List<WorkerEntity> workers = List.of(
            WorkerEntity.builder()
                .name("Budi Santoso")
                .avatar(bucket + "1.jpg")
                .age(38).experience(12).rating(4.9).totalReviews(124)
                .specializations(List.of("Pasang Keramik", "Plesteran", "Pondasi"))
                .location("Jakarta Selatan").pricePerDay(new BigDecimal("350000"))
                .isAvailable(true)
                .bio("Tukang berpengalaman dengan keahlian pasang keramik dan plesteran dinding. Mengerjakan lebih dari 200 proyek renovasi rumah di Jakarta.")
                .build(),

            WorkerEntity.builder()
                .name("Hendra Wijaya")
                .avatar(bucket + "2.jpg")
                .age(32).experience(7).rating(4.7).totalReviews(86)
                .specializations(List.of("Cat Dinding", "Gypsum", "Plafon"))
                .location("Jakarta Barat").pricePerDay(new BigDecimal("300000"))
                .isAvailable(true)
                .bio("Tukang muda spesialis pengecatan dinding dan pemasangan plafon gypsum. Hasil rapi dan bersih menggunakan bahan berkualitas.")
                .build(),

            WorkerEntity.builder()
                .name("Agus Setiawan")
                .avatar(bucket + "3.jpg")
                .age(50).experience(20).rating(5.0).totalReviews(210)
                .specializations(List.of("Bata & Dinding", "Struktur Beton", "Renovasi Total"))
                .location("Jakarta Pusat").pricePerDay(new BigDecimal("450000"))
                .isAvailable(false)
                .bio("Tukang senior dengan pengalaman 20 tahun di bidang konstruksi bangunan. Ahli dalam pengerjaan struktur beton dan renovasi total rumah.")
                .build(),

            WorkerEntity.builder()
                .name("Rizky Pratama")
                .avatar(bucket + "4.jpg")
                .age(35).experience(9).rating(4.8).totalReviews(97)
                .specializations(List.of("Instalasi Listrik", "Instalasi Air", "Sanitasi"))
                .location("Tangerang Selatan").pricePerDay(new BigDecimal("380000"))
                .isAvailable(true)
                .bio("Tukang spesialis instalasi listrik dan pipa air bersertifikasi. Berpengalaman dalam pemasangan sistem sanitasi dan renovasi kamar mandi.")
                .build(),

            WorkerEntity.builder()
                .name("Slamet Riyadi")
                .avatar(bucket + "5.jpg")
                .age(29).experience(6).rating(4.6).totalReviews(63)
                .specializations(List.of("Renovasi Dapur", "Pasang Wallpaper", "Finishing"))
                .location("Bekasi").pricePerDay(new BigDecimal("280000"))
                .isAvailable(true)
                .bio("Tukang ramah dan teliti dengan spesialisasi renovasi dapur dan finishing interior. Bekerja cepat dan rapi sesuai permintaan pelanggan.")
                .build(),

            WorkerEntity.builder()
                .name("Wahyu Kurniawan")
                .avatar(bucket + "6.jpg")
                .age(42).experience(15).rating(4.9).totalReviews(158)
                .specializations(List.of("Atap & Genteng", "Waterproofing", "Perbaikan Bocor"))
                .location("Depok").pricePerDay(new BigDecimal("500000"))
                .isAvailable(true)
                .bio("Tukang ahli perbaikan atap bocor dan waterproofing. Menggunakan material anti bocor berkualitas tinggi dengan garansi pekerjaan.")
                .build()
        );

        workerRepository.saveAll(workers);
        log.info("Seeded {} workers.", workers.size());
    }
}
