# Crédit 360° – Étape 3 : pièces justificatives (back-end Spring Boot + front Angular)

```
credit360/
├── backend/    API Spring Boot 3.4 (Java 21, Maven)
├── frontend/   Application Angular 21
└── docker-compose.yml   PostgreSQL (optionnel)
```

## Prérequis
- Java 21 et Maven 3.9+ (ou ouvrir `backend/` dans IntelliJ IDEA / Spring Tools : Maven est intégré)
- Node.js 22.12+ et npm

## 1. Lancer le back-end
```bash
cd backend
mvn spring-boot:run          # http://localhost:8080
```
Par défaut, la base H2 est stockée dans `backend/data/` : rien à installer. Au premier démarrage, les types de
crédit, 4 utilisateurs et 12 dossiers de démonstration sont créés.

### Avec PostgreSQL (optionnel)
```bash
docker compose up -d
export SPRING_PROFILES_ACTIVE=postgres
export DB_PASSWORD=credit360
export JWT_SECRET=$(openssl rand -base64 48)
export ADMIN_EMAIL=admin@exemple.ma ADMIN_PASSWORD='un-mot-de-passe-solide'   # premier administrateur
cd backend && mvn spring-boot:run
```
En profil `postgres`, les données de démonstration sont désactivées (`SEED_DEMO_DATA=true` pour les activer).

## 2. Lancer le front
```bash
cd frontend
npm install
npm start          # http://localhost:4200  (les appels /api sont redirigés vers le port 8080)
```

## Comptes de démonstration (mot de passe : demo1234)
| E-mail | Rôle | Accès |
|---|---|---|
| conseiller@credit360.ma | conseiller | ses propres dossiers uniquement |
| analyste@credit360.ma | analyste | tous les dossiers |
| responsable@credit360.ma | responsable | tous les dossiers |
| admin@credit360.ma | admin | tous les dossiers + Paramètres |

## API (toutes protégées par jeton JWT, sauf la connexion)
| Méthode | Route | Description |
|---|---|---|
| POST | /api/auth/login | Connexion → jeton + utilisateur |
| GET | /api/auth/me | Utilisateur connecté |
| GET | /api/types-credit | Types de crédit |
| GET | /api/dossiers?q=&statut=&type=&page=&taille= | Liste paginée et filtrée |
| GET | /api/dossiers/stats | Compteurs du tableau de bord |
| GET | /api/dossiers/recents | 5 derniers dossiers |
| GET | /api/dossiers/{reference} | Détail d'un dossier |
| POST | /api/dossiers | Création (validation côté serveur) |
| GET | /api/utilisateurs | Liste des utilisateurs (admin) |
| GET | /api/dossiers/{reference}/pieces | Checklist des pièces du dossier (déposées, manquantes, complétude) |
| POST | /api/dossiers/{reference}/pieces/{pieceId} | Dépôt ou remplacement d'un fichier (multipart, champ `fichier`) |
| DELETE | /api/dossiers/{reference}/pieces/{pieceId} | Suppression d'un fichier déposé |
| GET | /api/dossiers/{reference}/pieces/{pieceId}/fichier | Consultation du fichier |
| POST | /api/dossiers/{reference}/soumettre | Passe le dossier en « En analyse » si la checklist est complète |

## Pièces justificatives (étape 3)
- Pièces requises par type de crédit (obligatoires ou facultatives), créées au démarrage : liste indicative à valider avec la banque.
- Formats acceptés : PDF, JPG, PNG, 5 Mo maximum. Le format est contrôlé sur le contenu réel du fichier, pas sur son extension.
- Les fichiers sont stockés dans `backend/data/uploads/` (variable `STORAGE_DIR`) sous un nom généré : à sauvegarder avec la base.
- Dépôt, remplacement et suppression ne sont possibles que pour un dossier « À compléter ».
- Un dossier ne passe en « En analyse » que si toutes les pièces obligatoires sont déposées.
- Les dossiers de démonstration au-delà de « À compléter » reçoivent de petits PDF factices ; « Salma Rhazi » reste à compléter pour tester le dépôt.
- Si vous avez déjà lancé l'étape 2, supprimez `backend/data/` avant de relancer pour repartir d'une base propre.

## Notes importantes
- Le secret JWT par défaut (`application.yml`) sert uniquement au développement. En production, définir `JWT_SECRET`.
- `ddl-auto: update` crée le schéma automatiquement. À remplacer par des migrations Flyway au déploiement (étape 6).
- Les taux et plafonds des types de crédit sont fictifs.
- La référence d'un dossier est `CRD` + identifiant sur 5 chiffres.
