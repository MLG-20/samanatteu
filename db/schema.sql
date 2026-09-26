--
-- PostgreSQL database dump
--

\restrict YyQE2M5FdnjDu0oMk1agzibt7KYZyd7ANw1fLZK6FQHvn8Wcx7jatOuFw1WXIiP

-- Dumped from database version 16.15 (Ubuntu 16.15-0ubuntu0.24.04.1)
-- Dumped by pg_dump version 16.15 (Ubuntu 16.15-0ubuntu0.24.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: cotisation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cotisation (
    id bigint NOT NULL,
    participation_id bigint,
    cycle_id bigint,
    montant_du numeric(15,2),
    montant_paye numeric(15,2),
    date_paiement timestamp without time zone,
    mode_paiement character varying(20),
    reference character varying(100),
    statut character varying(20),
    created_at timestamp without time zone
);


--
-- Name: cotisation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cotisation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cotisation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cotisation_id_seq OWNED BY public.cotisation.id;


--
-- Name: cycle; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cycle (
    id bigint NOT NULL,
    tontine_id bigint,
    numero_cycle integer,
    date_debut date,
    date_fin_prevue date,
    date_fin_reelle date,
    montant_attendu numeric(15,2),
    montant_collecte numeric(15,2) DEFAULT 0,
    statut character varying(20),
    created_at timestamp without time zone
);


--
-- Name: cycle_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cycle_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cycle_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cycle_id_seq OWNED BY public.cycle.id;


--
-- Name: echeance_pret; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.echeance_pret (
    id bigint NOT NULL,
    pret_id bigint,
    numero_echeance integer,
    montant_du numeric(15,2),
    montant_paye numeric(15,2) DEFAULT 0,
    date_echeance date,
    date_paiement timestamp without time zone,
    statut character varying(20)
);


--
-- Name: echeance_pret_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.echeance_pret_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: echeance_pret_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.echeance_pret_id_seq OWNED BY public.echeance_pret.id;


--
-- Name: import_membres; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.import_membres (
    id bigint NOT NULL,
    tontine_id bigint,
    fichier_nom character varying(255),
    nb_membres_total integer DEFAULT 0,
    nb_importes integer DEFAULT 0,
    nb_erreurs integer DEFAULT 0,
    erreurs_detail text,
    statut character varying(20),
    created_at timestamp without time zone
);


--
-- Name: import_membres_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.import_membres_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: import_membres_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.import_membres_id_seq OWNED BY public.import_membres.id;


--
-- Name: invitation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invitation (
    id bigint NOT NULL,
    tontine_id bigint,
    token character varying(255),
    telephone character varying(20),
    email character varying(150),
    prenom_pre_rempli character varying(100),
    nom_pre_rempli character varying(100),
    nombre_parts integer DEFAULT 1,
    expire_at timestamp without time zone,
    statut character varying(20),
    created_at timestamp without time zone
);


--
-- Name: invitation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.invitation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: invitation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.invitation_id_seq OWNED BY public.invitation.id;


--
-- Name: notification; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification (
    id bigint NOT NULL,
    destinataire_id bigint,
    titre character varying(200),
    message text,
    type character varying(20),
    statut character varying(20),
    date_envoi timestamp without time zone,
    created_at timestamp without time zone
);


--
-- Name: notification_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notification_id_seq OWNED BY public.notification.id;


--
-- Name: participation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.participation (
    id bigint NOT NULL,
    membre_id bigint,
    tontine_id bigint,
    nombre_parts integer,
    date_adhesion date,
    statut character varying(20),
    ordre_inscription integer
);


--
-- Name: participation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.participation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: participation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.participation_id_seq OWNED BY public.participation.id;


--
-- Name: pret; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pret (
    id bigint NOT NULL,
    membre_id bigint,
    tontine_id bigint,
    gestionnaire_id bigint,
    montant numeric(15,2),
    taux_interet numeric(15,2) DEFAULT 0,
    nb_echeances integer,
    date_accord date,
    statut character varying(20),
    created_at timestamp without time zone
);


--
-- Name: pret_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pret_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pret_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pret_id_seq OWNED BY public.pret.id;


--
-- Name: tirage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tirage (
    id bigint NOT NULL,
    cycle_id bigint,
    participation_id bigint,
    montant_gagne numeric(15,2),
    date_tirage timestamp without time zone,
    date_versement timestamp without time zone,
    statut character varying(20),
    created_at timestamp without time zone
);


--
-- Name: tirage_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tirage_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tirage_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tirage_id_seq OWNED BY public.tirage.id;


--
-- Name: tontine; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tontine (
    id bigint NOT NULL,
    nom character varying(150) NOT NULL,
    montant_part numeric(10,2) NOT NULL,
    frequence character varying(20) NOT NULL,
    nb_cycles_total integer NOT NULL,
    description text,
    jour_cotisation integer,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    statut character varying(20),
    gestionnaire_id bigint
);


--
-- Name: tontine_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tontine_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tontine_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tontine_id_seq OWNED BY public.tontine.id;


--
-- Name: transaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transaction (
    id bigint NOT NULL,
    membre_id bigint,
    tontine_id bigint,
    type character varying(20),
    montant numeric(15,2),
    sens character varying(20),
    reference_id bigint,
    description text,
    date_transaction timestamp without time zone,
    created_at timestamp without time zone
);


--
-- Name: transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.transaction_id_seq OWNED BY public.transaction.id;


--
-- Name: utilisateur; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.utilisateur (
    id bigint NOT NULL,
    nom character varying(100),
    prenom character varying(100),
    telephone character varying(20) NOT NULL,
    email character varying(150),
    mot_de_passe character varying(255),
    role character varying(20),
    actif boolean DEFAULT true,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);


--
-- Name: utilisateur_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.utilisateur_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: utilisateur_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.utilisateur_id_seq OWNED BY public.utilisateur.id;


--
-- Name: cotisation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotisation ALTER COLUMN id SET DEFAULT nextval('public.cotisation_id_seq'::regclass);


--
-- Name: cycle id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cycle ALTER COLUMN id SET DEFAULT nextval('public.cycle_id_seq'::regclass);


--
-- Name: echeance_pret id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.echeance_pret ALTER COLUMN id SET DEFAULT nextval('public.echeance_pret_id_seq'::regclass);


--
-- Name: import_membres id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_membres ALTER COLUMN id SET DEFAULT nextval('public.import_membres_id_seq'::regclass);


--
-- Name: invitation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitation ALTER COLUMN id SET DEFAULT nextval('public.invitation_id_seq'::regclass);


--
-- Name: notification id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification ALTER COLUMN id SET DEFAULT nextval('public.notification_id_seq'::regclass);


--
-- Name: participation id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.participation ALTER COLUMN id SET DEFAULT nextval('public.participation_id_seq'::regclass);


--
-- Name: pret id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pret ALTER COLUMN id SET DEFAULT nextval('public.pret_id_seq'::regclass);


--
-- Name: tirage id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tirage ALTER COLUMN id SET DEFAULT nextval('public.tirage_id_seq'::regclass);


--
-- Name: tontine id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tontine ALTER COLUMN id SET DEFAULT nextval('public.tontine_id_seq'::regclass);


--
-- Name: transaction id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction ALTER COLUMN id SET DEFAULT nextval('public.transaction_id_seq'::regclass);


--
-- Name: utilisateur id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilisateur ALTER COLUMN id SET DEFAULT nextval('public.utilisateur_id_seq'::regclass);


--
-- Name: cotisation cotisation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotisation
    ADD CONSTRAINT cotisation_pkey PRIMARY KEY (id);


--
-- Name: cycle cycle_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cycle
    ADD CONSTRAINT cycle_pkey PRIMARY KEY (id);


--
-- Name: echeance_pret echeance_pret_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.echeance_pret
    ADD CONSTRAINT echeance_pret_pkey PRIMARY KEY (id);


--
-- Name: import_membres import_membres_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_membres
    ADD CONSTRAINT import_membres_pkey PRIMARY KEY (id);


--
-- Name: invitation invitation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitation
    ADD CONSTRAINT invitation_pkey PRIMARY KEY (id);


--
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: participation participation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.participation
    ADD CONSTRAINT participation_pkey PRIMARY KEY (id);


--
-- Name: pret pret_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pret
    ADD CONSTRAINT pret_pkey PRIMARY KEY (id);


--
-- Name: tirage tirage_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tirage
    ADD CONSTRAINT tirage_pkey PRIMARY KEY (id);


--
-- Name: tontine tontine_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tontine
    ADD CONSTRAINT tontine_pkey PRIMARY KEY (id);


--
-- Name: transaction transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT transaction_pkey PRIMARY KEY (id);


--
-- Name: utilisateur utilisateur_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_email_key UNIQUE (email);


--
-- Name: utilisateur utilisateur_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_pkey PRIMARY KEY (id);


--
-- Name: utilisateur utilisateur_telephone_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_telephone_key UNIQUE (telephone);


--
-- Name: cotisation cotisation_cycle_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotisation
    ADD CONSTRAINT cotisation_cycle_id_fkey FOREIGN KEY (cycle_id) REFERENCES public.cycle(id);


--
-- Name: cotisation cotisation_participation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotisation
    ADD CONSTRAINT cotisation_participation_id_fkey FOREIGN KEY (participation_id) REFERENCES public.participation(id);


--
-- Name: cycle cycle_tontine_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cycle
    ADD CONSTRAINT cycle_tontine_id_fkey FOREIGN KEY (tontine_id) REFERENCES public.tontine(id);


--
-- Name: echeance_pret echeance_pret_pret_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.echeance_pret
    ADD CONSTRAINT echeance_pret_pret_id_fkey FOREIGN KEY (pret_id) REFERENCES public.pret(id);


--
-- Name: import_membres import_membres_tontine_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.import_membres
    ADD CONSTRAINT import_membres_tontine_id_fkey FOREIGN KEY (tontine_id) REFERENCES public.tontine(id);


--
-- Name: invitation invitation_tontine_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitation
    ADD CONSTRAINT invitation_tontine_id_fkey FOREIGN KEY (tontine_id) REFERENCES public.tontine(id);


--
-- Name: notification notification_destinataire_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_destinataire_id_fkey FOREIGN KEY (destinataire_id) REFERENCES public.utilisateur(id);


--
-- Name: participation participation_membre_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.participation
    ADD CONSTRAINT participation_membre_id_fkey FOREIGN KEY (membre_id) REFERENCES public.utilisateur(id);


--
-- Name: participation participation_tontine_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.participation
    ADD CONSTRAINT participation_tontine_id_fkey FOREIGN KEY (tontine_id) REFERENCES public.tontine(id);


--
-- Name: pret pret_gestionnaire_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pret
    ADD CONSTRAINT pret_gestionnaire_id_fkey FOREIGN KEY (gestionnaire_id) REFERENCES public.utilisateur(id);


--
-- Name: pret pret_membre_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pret
    ADD CONSTRAINT pret_membre_id_fkey FOREIGN KEY (membre_id) REFERENCES public.utilisateur(id);


--
-- Name: pret pret_tontine_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pret
    ADD CONSTRAINT pret_tontine_id_fkey FOREIGN KEY (tontine_id) REFERENCES public.tontine(id);


--
-- Name: tirage tirage_cycle_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tirage
    ADD CONSTRAINT tirage_cycle_id_fkey FOREIGN KEY (cycle_id) REFERENCES public.cycle(id);


--
-- Name: tirage tirage_participation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tirage
    ADD CONSTRAINT tirage_participation_id_fkey FOREIGN KEY (participation_id) REFERENCES public.participation(id);


--
-- Name: tontine tontine_gestionnaire_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tontine
    ADD CONSTRAINT tontine_gestionnaire_id_fkey FOREIGN KEY (gestionnaire_id) REFERENCES public.utilisateur(id);


--
-- Name: transaction transaction_membre_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT transaction_membre_id_fkey FOREIGN KEY (membre_id) REFERENCES public.utilisateur(id);


--
-- Name: transaction transaction_tontine_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT transaction_tontine_id_fkey FOREIGN KEY (tontine_id) REFERENCES public.tontine(id);


--
-- PostgreSQL database dump complete
--

\unrestrict YyQE2M5FdnjDu0oMk1agzibt7KYZyd7ANw1fLZK6FQHvn8Wcx7jatOuFw1WXIiP

