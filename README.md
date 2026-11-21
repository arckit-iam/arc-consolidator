# ARC Consolidator

Service de normalisation de la plateforme [Instant Access Provider](https://github.com/arckit-iam/InstantAccessPublisher). Il consomme les événements bruts des sources IAM sur les topics Kafka `iam.raw.*`, les convertit dans le modèle commun ARC (Identity / Entitlement / Assignment) et publie le résultat sur les topics `arc.sync.*`.

## Fonctionnement

```text
iam.raw.activedirectory ──► RawAdEventConsumer   ──► ArcConsolidatorService   ──┐
iam.raw.isds            ──► RawIsdsEventConsumer ──► IsdsConsolidatorService ──┤
                                                                                  ▼
                                                                         ArcEventPublisher
                                                                                  │
                                              arc.sync.identity / arc.sync.entitlement / arc.sync.assignment
```

- Un consumer Kafka par source, un service applicatif par source, un mapper dédié par source (`ActiveDirectoryEventMapper`, `IsdsEventMapper`).
- Chaque événement brut produit un ou plusieurs DTO (`IdentityEventDTO`, `EntitlementEventDTO`, `AssignmentEventDTO`), publiés sur le topic correspondant.
- Les types d'événements sont `CREATED`, `MODIFIED`, `DELETED`, `ENABLED`, `DISABLED`, `RENAMED`, `ADDED`, `REMOVED` ou `UNKNOWN`. Le type d'objet est porté par le topic.

## Configuration

| Variable | Défaut | Rôle |
|---|---|---|
| `SPRING_KAFKA_CONSUMER_BOOTSTRAP_SERVERS` | `localhost:9092` | Broker Kafka (consommation) |
| `SPRING_KAFKA_PRODUCER_BOOTSTRAP_SERVERS` | `localhost:9092` | Broker Kafka (publication) |
| `IAM_TOPICS_ACTIVEDIRECTORY` | `iam.raw.activedirectory` | Topic brut Active Directory |
| `ARC_TOPIC_IDENTITY` | `arc.sync.identity` | Topic des identités |
| `ARC_TOPIC_ENTITLEMENT` | `arc.sync.entitlement` | Topic des droits |
| `ARC_TOPIC_ASSIGNMENT` | `arc.sync.assignment` | Topic des affectations |

Le consumer ISDS écoute `iam.raw.isds`, modifiable avec la propriété `iam.topics.isds`.

Un exemple d'événement Active Directory brut est fourni dans [exemple-ad.json](exemple-ad.json).

## Architecture

Le projet suit l'architecture hexagonale. Le domaine ne dépend d'aucun framework.

```text
src/main/java/com/instantaccessprovider/arcconsolidator/
├── domain/port/              Ports (ArcEventOutputPort)
├── application/service/      Services applicatifs par source
├── mapper/                   Mappers brut -> DTO, par source
├── dto/                      DTO par topic de sortie
├── model/                    Modèles métier (ArcEventType)
├── kafka/consumer/           Adapters entrants (un par source)
├── kafka/publisher/          Adapter sortant (ArcEventPublisher)
└── infrastructure/config/    Configuration Spring et Kafka
```

### Ajouter une source

1. Créer un mapper dédié (par exemple `RacfEventMapper`).
2. Créer un service applicatif dédié.
3. Créer un consumer Kafka qui écoute `iam.raw.<source>`.
4. Réutiliser les DTO existants, ou en ajouter si le modèle l'exige.

## Développement

Prérequis : JDK 25 et Maven 3.9.

```bash
mvn test            # tests unitaires
mvn package         # construit target/arc-consolidator-*.jar
docker build -t arc-consolidator .
```

## Documentation

- [arc-docs](https://github.com/arckit-iam/arc-docs) : architecture de la plateforme.
  - [Modèle abstrait IAM](https://github.com/arckit-iam/arc-docs/blob/main/docs/00-modele-abstrait.md) : entités, catalogue d'événements `arc.sync.*`, formats CloudEvents et correspondances par source.
  - [Architecture globale](https://github.com/arckit-iam/arc-docs/blob/main/docs/02-architecture-globale.md).
- [arc-kubeorchestrator](https://github.com/arckit-iam/arc-kubeorchestrator/tree/main/docs) : déploiement (Helm, Flux, CI/CD).

## Contribuer et sécurité

Voir [CONTRIBUTING.md](CONTRIBUTING.md) et [SECURITY.md](SECURITY.md).

## Licence

Copyright © 2026 Dédale Solutions

Ce projet est distribué sous la licence [EUPL-1.2](LICENSE) (European Union Public Licence v. 1.2).
