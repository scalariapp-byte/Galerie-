# Galerie

Application de galerie Android native (Kotlin + Jetpack Compose), rapide et complète.

## Fonctions
- Chronologie groupée par jour/mois, pincer pour 2 à 8 colonnes, défilement rapide avec date
- Sélection multiple par appui long + glisser ; partager, favori, déplacer/copier, supprimer
- Albums + collections automatiques : Favoris, Vidéos, Captures, GIF, HD, Panoramas, Récents, Gros fichiers, Doublons, Corbeille
- Recherche (nom, album, date, type)
- Visionneuse : zoom profond, vidéo (vitesse, muet, ±10 s), diaporama, EXIF + carte, modifier, fond d'écran, renommer
- Libérer de l'espace, corbeille avec restauration, thèmes clair/sombre/AMOLED

## Compiler
```
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```
Android 11+ requis. Le keystore `keystore/galerie.keystore` est une clé de signature de développement (mot de passe `galerie`), sans valeur secrète.
