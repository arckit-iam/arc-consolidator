# 00 — Modèle Abstrait IAM (Generic Identity & Access Model) InstantAccessProvider

## 1. Motivation

L'architecture initiale était centrée sur Active Directory. Mais les mêmes problèmes se posent pour :

| Système | "Groupe" | "Utilisateur" | "Affectation" |
|---|---|---|---|
| Active Directory | Security Group | User account | memberOf |
| Application A | Profil applicatif | Compte appli | Utilisateur → Profil |
| OpenLDAP | groupOfNames | inetOrgPerson | member |
| Azure AD / Entra ID | Azure AD Group | User | memberOf |
| AWS IAM | IAM Role / Group | IAM User | Role attachment |
| Kubernetes RBAC | ClusterRole | ServiceAccount | RoleBinding |
| Application B (ITSM) | Permission level | Agent | Agent → Permission |

**Observation clé** : dans tous ces systèmes, on retrouve toujours les mêmes **trois entités abstraites** et les mêmes **types d'événements**. Le schéma Kafka doit travailler avec ces abstractions, pas avec les concepts natifs de chaque système.

---

## 2. Les trois entités abstraites

```
┌─────────────────────────────────────────────────────────────┐
│                    MODÈLE ABSTRAIT IAM                       │
│                                                             │
│   ┌─────────────┐        ┌─────────────────────────┐       │
│   │  IDENTITY   │        │     ENTITLEMENT         │       │
│   │             │        │                         │       │
│   │ Qui peut    │        │ Ce qui donne accès       │       │
│   │ agir        │        │                         │       │
│   │             │        │ = AD Security Group      │       │
│   │ = User AD   │        │ = Profil applicatif      │       │
│   │ = Compte    │        │ = LDAP groupOfNames      │       │
│   │   applicatif│        │ = AWS IAM Role           │       │
│   │ = Service   │        │ = K8s ClusterRole        │       │
│   │   account   │        │ = Permission level       │       │
│   └──────┬──────┘        └────────────┬────────────┘       │
│          │                            │                     │
│          └────────────┬───────────────┘                     │
│                       │                                     │
│              ┌────────▼────────┐                            │
│              │   ASSIGNMENT    │                            │
│              │                 │                            │
│              │ Le lien entre   │                            │
│              │ Identity et     │                            │
│              │ Entitlement     │                            │
│              │                 │                            │
│              │ = memberOf AD   │                            │
│              │ = User→Profil   │                            │
│              │ = RoleBinding   │                            │
│              │                 │                            │
│              │ + scope?        │                            │
│              │ (périmètre      │                            │
│              │  métier optionnel)                           │
│              └─────────────────┘                            │
└─────────────────────────────────────────────────────────────┘
```

### 2.1 Identity

> **Une entité qui peut être authentifiée et à laquelle on peut affecter des droits.**

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Identifiant stable généré par la plateforme |
| `externalId` | string | Identifiant dans le système source (SID, UID, UUID appli…) |
| `sourceSystem` | string | Système d'origine (`activedirectory`, `app-a`, `ldap`, `entra-id`) |
| `type` | enum | `user` / `service` / `machine` / `robot` / `guest` |
| `displayName` | string | Nom lisible |
| `metadata` | map | Champs additionnels libres (department, email…) |

**Mapping par système source** :

| Système | externalId | type | metadata exemples |
|---|---|---|---|
| Active Directory | SID (`S-1-5-21-…`) | `user` ou `service` | `samAccountName`, `upn`, `department` |
| Application A | UUID applicatif | `user` | `login`, `email`, `tenant` |
| OpenLDAP | `dn` de l'entrée | `user` | `uid`, `mail`, `ou` |
| Azure AD | Object ID (GUID) | `user` / `guest` | `userPrincipalName` |

---

### 2.2 Entitlement

> **Un ensemble nommé de droits ou d'accès qui peut être affecté à des identités. C'est l'unité d'octroi d'accès.**

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Identifiant stable généré par la plateforme |
| `externalId` | string | Identifiant dans le système source (SID groupe, ID profil appli…) |
| `sourceSystem` | string | Système d'origine |
| `name` | string | Nom lisible (`Domain Admins`, `PROFIL_GESTIONNAIRE`, `admin-role`) |
| `type` | enum | `security-group` / `distribution-group` / `app-profile` / `role` / `permission-set` |
| `sensitivityLevel` | enum | `LOW` / `MEDIUM` / `HIGH` / `CRITICAL` |
| `owner` | string | Propriétaire fonctionnel |
| `metadata` | map | Champs additionnels libres |

**Mapping par système source** :

| Système | externalId | type | Exemple name |
|---|---|---|---|
| AD groupe sécurité | SID du groupe | `security-group` | `Domain Admins` |
| AD groupe distribution | SID du groupe | `distribution-group` | `DL-Finance-All` |
| Application A | ID profil (int/UUID) | `app-profile` | `PROFIL_GESTIONNAIRE` |
| OpenLDAP group | `dn` du groupe | `security-group` | `cn=admins,ou=groups,…` |
| AWS IAM | ARN du rôle | `role` | `arn:aws:iam::…:role/Admin` |
| K8s | `name` du ClusterRole | `role` | `cluster-admin` |

---

### 2.3 Assignment

> **Le lien effectif entre une Identity et un Entitlement, à un instant donné, éventuellement restreint à un périmètre métier (scope).**

| Champ | Type | Description |
|---|---|---|
| `id` | UUID | Identifiant stable de l'affectation |
| `identityId` | UUID | Ref vers l'Identity (id plateforme) |
| `entitlementId` | UUID | Ref vers l'Entitlement (id plateforme) |
| `sourceSystem` | string | Système source de cette affectation |
| `grantedBy` | Identity ref | Qui a fait l'affectation |
| `grantedAt` | datetime | Horodatage de l'affectation |
| `expiresAt` | datetime? | Date d'expiration (si applicable) |
| `scope` | Scope? | **Optionnel** — Périmètre métier de l'affectation (voir §2.4) |
| `metadata` | map | Champs additionnels libres |

---

### 2.4 Scope (optionnel)

> **Un périmètre métier qui restreint l'exercice d'un Entitlement à une portion de données ou d'organisation.**

Le scope n'est pas un Entitlement : il ne définit pas *ce qu'on peut faire*, il restreint *sur quoi on peut le faire*. Il est porté par l'Assignment, pas par l'Entitlement, car le même droit peut s'appliquer avec des périmètres différents selon les personnes.

**Exemples concrets :**

| Contexte | Entitlement | Scope |
|---|---|---|
| RH multinationale | `PROFIL_RH_MANAGER` | `region: EMEA` |
| Gestion documentaire | `ACCES_CONTRATS` | `service: Direction Juridique` |
| Application financière | `ROLE_VALIDEUR` | `entity: Filiale-FR-01` |
| Active Directory | `Domain Admins` | *(pas de scope — s'applique au domaine entier)* |
| Support N2 | `PROFIL_SUPPORT` | `country: FR`, `tier: premium` |

**Structure du Scope :**

| Champ | Type | Description |
|---|---|---|
| `type` | string | Type de dimension (`region`, `entity`, `service`, `country`, `tenant`…) |
| `value` | string \| string[] | Valeur(s) du périmètre |
| `label` | string? | Libellé lisible |

Plusieurs dimensions peuvent être combinées dans un même scope :

```json
"scope": {
  "dimensions": [
    { "type": "region",  "value": "EMEA",              "label": "Europe, Moyen-Orient, Afrique" },
    { "type": "entity",  "value": ["FR-01", "FR-02"],  "label": "Filiales France" }
  ]
}
```

> Le scope est **absent** lorsque l'Entitlement s'applique sans restriction de périmètre. Les consommateurs qui ne gèrent pas le scope doivent ignorer le champ sans erreur (rétrocompatibilité garantie).

---

## 3. Les événements génériques

Quel que soit le système source, les événements se réduisent à **neuf types** :

```
IDENTITY EVENTS
  iam.identity.created      ← 4720 (AD) / création compte applicatif
  iam.identity.updated      ← 4738 (AD) / modification profil utilisateur
  iam.identity.deleted      ← 4726 (AD) / suppression compte
  iam.identity.enabled      ← 4722 (AD) / activation
  iam.identity.disabled     ← 4725 (AD) / désactivation

ENTITLEMENT EVENTS
  iam.entitlement.created   ← 4727/4731/4754 (AD) / création profil appli
  iam.entitlement.updated   ← modification groupe/profil
  iam.entitlement.deleted   ← 4730/4734/4758 (AD) / suppression profil

ASSIGNMENT EVENTS
  iam.assignment.added      ← 4728/4732/4756 (AD) / User → Profil applicatif
  iam.assignment.removed    ← 4729/4733/4757 (AD) / retrait
```

---

## 4. Architecture des sources (multi-source)

Le principe fondamental : la collecte est assurée par **Fluent Bit** ou **CAESAR** sur les serveurs de collecte (WEC, collecteurs syslog, etc.). Le traitement métier est assuré en aval par le Stream Processor.

```
[AD Domain Controllers]  ──[WEF]──────────────────────────► WEC ──► Fluent Bit ──► iam.raw.activedirectory
[ISVD / équipements]     ──[Syslog]──────────────────────► Fluent Bit ───────────► iam.raw.syslog
[Serveurs Linux]         ──[Syslog / auditd]────────────► Fluent Bit ───────────► iam.raw.syslog
[Applications]           ──[Webhook / REST / CDC]───────► Fluent Bit / bridge ──► iam.raw.{sourceId}
[OpenLDAP / Entra ID]    ──[DirSync / Graph API]────────► Collecteur dédié ─────► iam.raw.ldap

       tous les topics raw iam.raw.*
           │
      ┌──────────▼──────────┐
      │  Stream Processor   │
      │  (normalise/enrichit)
      └──────────┬──────────┘
           │
  ┌──────────────┼──────────────┬──────────────┬────────────────┐
  ▼              ▼              ▼              ▼                ▼
[iam.identity] [iam.entitlement] [iam.assignment] [iam.alert] [iam.consolidated]
                      [iam.audit]

iam.consolidated ──► InstantAccessPublisher (Java) ──► PostgreSQL (etat T0)
```

**Responsabilites de Fluent Bit :**
- Ecoute des protocoles de collecte (winlog/syslog/http/tail)
- Enrichissement technique minimal (tags source/zone/collector)
- Publication fiable dans `iam.raw.{sourceId}`

**Responsabilites du Stream Processor :**
- Mapping metier vers le modele IAM abstrait
- Enrichissement (LDAP, CMDB, UO AD)
- Publication des topics normalises et du topic de projection `iam.consolidated`

**Pourquoi un topic raw par source ?**
- Rejouer une seule source sans toucher aux autres
- Schémas bruts différents par source — pas de couplage
- Isolation des pannes : un collecteur ou un flux en erreur n'affecte pas les autres

---

## 5. Registre des sources (Source Registry)

Chaque système source doit s'enregistrer dans un **Source Registry** avant de produire des événements. Ce registre est la clé de l'interopérabilité.

```json
{
  "sourceId": "ad://corp.example.com",
  "sourceType": "activedirectory",
  "displayName": "Active Directory - corp.example.com",
  "domain": "corp.example.com",
  "collecteur": "collecteur-ad-v2",
  "entityMapping": {
    "identity": "user account",
    "entitlement": "security group",
    "assignment": "group membership"
  },
  "sensitiveEntitlements": [
    { "namePattern": "Domain Admins", "level": "CRITICAL" },
    { "namePattern": "VPN-*", "level": "MEDIUM" }
  ],
  "registeredAt": "2026-01-15T09:00:00Z"
}
```

```json
{
  "sourceId": "app://app-a.example.com",
  "sourceType": "business-application",
  "displayName": "Application A — Gestion RH",
  "collecteur": "collecteur-app-webhook-v1",
  "entityMapping": {
    "identity": "utilisateur applicatif",
    "entitlement": "profil applicatif",
    "assignment": "affectation utilisateur → profil"
  },
  "sensitiveEntitlements": [
    { "namePattern": "PROFIL_ADMIN_*", "level": "HIGH" },
    { "namePattern": "PROFIL_EXPORT_PAIE", "level": "CRITICAL" }
  ],
  "registeredAt": "2026-02-01T10:30:00Z"
}
```

---

## 6. Schéma CloudEvents générique

### 6.1 Événement `iam.assignment.added` (universel)

```json
{
  "specversion": "1.0",
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "source": "ad://corp.example.com",
  "type": "com.example.iam.assignment.added",
  "time": "2026-04-02T14:32:11.123Z",
  "datacontenttype": "application/json",
  "iamsourcetype": "activedirectory",

  "data": {
    "assignment": {
      "id": "a1b2c3d4-...",
      "grantedAt": "2026-04-02T14:32:11.123Z",
      "grantedBy": {
        "externalId": "S-1-5-21-...-500",
        "displayName": "admin.service",
        "sourceSystem": "ad://corp.example.com"
      }
    },
    "identity": {
      "id": "f47ac10b-...",
      "externalId": "S-1-5-21-...-1105",
      "sourceSystem": "ad://corp.example.com",
      "type": "user",
      "displayName": "John Doe",
      "metadata": {
        "samAccountName": "john.doe",
        "upn": "john.doe@corp.example.com",
        "department": "IT"
      }
    },
    "entitlement": {
      "id": "e7d3a021-...",
      "externalId": "S-1-5-21-...-512",
      "sourceSystem": "ad://corp.example.com",
      "type": "security-group",
      "name": "Domain Admins",
      "sensitivityLevel": "CRITICAL",
      "metadata": {
        "distinguishedName": "CN=Domain Admins,CN=Users,DC=corp,DC=example,DC=com",
        "nativeEventId": 4728
      }
    },
    "context": {
      "isOutsideBusinessHours": false,
      "isPrivilegedEntitlement": true,
      "sourceNativeEventId": "4728"
    }
  }
}
```

### 6.2 Assignment avec scope — Application RH (exemple)

```json
{
  "specversion": "1.0",
  "id": "3e4f5a06-cd23-...",
  "source": "app://sirh.example.com",
  "type": "com.example.iam.assignment.added",
  "time": "2026-04-03T09:15:00.000Z",
  "iamsourcetype": "business-application",

  "data": {
    "assignment": {
      "id": "c1d2e3f4-...",
      "grantedAt": "2026-04-03T09:15:00.000Z",
      "grantedBy": {
        "externalId": "admin-rh-001",
        "displayName": "Administrateur RH",
        "sourceSystem": "app://sirh.example.com"
      },
      "scope": {
        "dimensions": [
          { "type": "region", "value": "EMEA", "label": "Europe, Moyen-Orient, Afrique" }
        ]
      }
    },
    "identity": {
      "id": "a1b2c3d4-...",
      "externalId": "emp-00789",
      "sourceSystem": "app://sirh.example.com",
      "type": "user",
      "displayName": "Marie Dupont",
      "metadata": { "email": "m.dupont@corp.example.com" }
    },
    "entitlement": {
      "id": "b2c3d4e5-...",
      "externalId": "profile-rh-manager",
      "sourceSystem": "app://sirh.example.com",
      "type": "app-profile",
      "name": "PROFIL_RH_MANAGER",
      "sensitivityLevel": "HIGH"
    },
    "context": {
      "isOutsideBusinessHours": false,
      "isPrivilegedEntitlement": true,
      "sourceNativeEventId": "SIRH_ASSIGN_884421"
    }
  }
}
```

> Sans le champ `scope`, l'affectation s'applique sans restriction de périmètre. L'absence de `scope` est un état valide et ne doit pas être interprété comme une erreur.

### 6.3 Même événement — Application A (profil applicatif)

```json
{
  "specversion": "1.0",
  "id": "9f1e3a02-ab12-...",
  "source": "app://app-a.example.com",
  "type": "com.example.iam.assignment.added",
  "time": "2026-04-02T15:01:00.000Z",
  "iamsourcetype": "business-application",

  "data": {
    "assignment": {
      "id": "b2c3d4e5-...",
      "grantedAt": "2026-04-02T15:01:00.000Z",
      "grantedBy": {
        "externalId": "user-admin-001",
        "displayName": "Administrateur App A",
        "sourceSystem": "app://app-a.example.com"
      }
    },
    "identity": {
      "id": "c3d4e5f6-...",
      "externalId": "user-00234",
      "sourceSystem": "app://app-a.example.com",
      "type": "user",
      "displayName": "Jane Smith",
      "metadata": {
        "login": "jsmith",
        "email": "jane.smith@corp.example.com",
        "tenant": "corp"
      }
    },
    "entitlement": {
      "id": "d4e5f6a7-...",
      "externalId": "profile-003",
      "sourceSystem": "app://app-a.example.com",
      "type": "app-profile",
      "name": "PROFIL_EXPORT_PAIE",
      "sensitivityLevel": "CRITICAL",
      "metadata": {
        "description": "Accès export données de paie",
        "ownerTeam": "RH-Admin"
      }
    },
    "context": {
      "isOutsideBusinessHours": true,
      "isPrivilegedEntitlement": true,
      "sourceNativeEventId": "APP_PROFILE_ASSIGN_V2"
    }
  }
}
```

**Les trois événements ont le même `type`, le même schéma, les mêmes règles de détection SOC.**  
Seul `source` et `data.entitlement.type` différencient l'origine.

---

## 7. Tables de correspondance par source

### Active Directory

| AD natif | Abstraction IAM | type champ |
|---|---|---|
| UserAccount | Identity | `user` ou `service` |
| Security Group | Entitlement | `security-group` |
| Distribution Group | Entitlement | `distribution-group` |
| memberOf | Assignment | — |
| Event 4720 | `iam.identity.created` | — |
| Event 4727/4731/4754 | `iam.entitlement.created` | — |
| Event 4728/4732/4756 | `iam.assignment.added` | — |
| Event 4729/4733/4757 | `iam.assignment.removed` | — |

### Application générique (REST / Webhook / DB CDC)

| App natif | Abstraction IAM | type champ |
|---|---|---|
| Utilisateur applicatif | Identity | `user` |
| Profil / Rôle applicatif | Entitlement | `app-profile` ou `role` |
| Affectation User → Profil | Assignment | — |
| Création profil | `iam.entitlement.created` | — |
| Affectation profil | `iam.assignment.added` | — |
| Retrait profil | `iam.assignment.removed` | — |

### OpenLDAP

| LDAP natif | Abstraction IAM | type champ |
|---|---|---|
| `inetOrgPerson` | Identity | `user` |
| `groupOfNames` | Entitlement | `security-group` |
| `member` attribute | Assignment | — |
| DirSync add entry | `iam.identity.created` | — |
| DirSync modify member | `iam.assignment.added/removed` | — |

---

# 7. Prompt de gestion des événements AD (arc-consolidator)

## Événements AD gérés

Le projet gère les événements Active Directory suivants (Event IDs) :

- 4720 : Création de compte utilisateur
- 4722 : Activation de compte utilisateur
- 4725 : Désactivation de compte utilisateur
- 4726 : Suppression de compte utilisateur
- 4727 : Création de groupe de sécurité
- 4728 : Ajout d’un membre à un groupe
- 4729 : Suppression d’un membre d’un groupe
- 4730 : Suppression de groupe de sécurité
- 4731 : Modification de groupe de sécurité
- 4732 : Ajout d’un membre à un groupe local
- 4733 : Suppression d’un membre d’un groupe local
- 4734 : Suppression de groupe local
- 4738 : Modification de compte utilisateur (affichage détaillé des attributs modifiés)
- 4754 : Création de groupe universel
- 4756 : Ajout d’un membre à un groupe universel
- 4757 : Suppression d’un membre d’un groupe universel
- 4758 : Suppression de groupe universel
- 4781 : Renommage de compte utilisateur

## Mapping générique

Chaque événement AD est mappé sur un événement métier générique du modèle IAM abstrait :

- **IDENTITY** : création, modification, suppression, activation, désactivation, renommage
- **ENTITLEMENT** : création, modification, suppression de groupe
- **ASSIGNMENT** : ajout ou retrait d’un utilisateur dans un groupe (avec scope optionnel)

## Cas d’usage CRUD Groupe et Affectation

### Groupe (Entitlement)
- Création : 4727, 4731, 4754
- Modification : 4731
- Suppression : 4730, 4734, 4758

### Affectation (Assignment)
- Ajout d’un utilisateur dans un groupe : 4728, 4732, 4756
- Retrait d’un utilisateur d’un groupe : 4729, 4733, 4757
- Le champ `scope` est optionnel et permet de restreindre l’affectation à un périmètre métier.

### Exemple d’événement Assignment avec scope
```json
{
  "eventType": "iam.assignment.added",
  "identityId": "...",
  "entitlementId": "...",
  "scope": {
    "dimensions": [
      { "type": "region", "value": "EMEA" },
      { "type": "entity", "value": ["FR-01", "FR-02"] }
    ]
  }
}
```

### Exemple d’événement Assignment sans scope
```json
{
  "eventType": "iam.assignment.added",
  "identityId": "...",
  "entitlementId": "..."
}
```
