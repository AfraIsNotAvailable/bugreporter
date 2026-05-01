## Lucru in echipa (folosind Git Branching)

- Fiecare membru al echipei va lucra pe o ramură (branch) separată, denumită după funcționalitatea pe care o
  implementează (ex: `sisteme-de-baza`, `motor-raportare`, `interactiuni-sociale`).

### Workflow-ul de lucru (comenzi Git):

1. **Crearea unei ramuri noi:**
   ```bash
   git checkout -b nume-ramura
   ```
2. **Adăugarea și comiterea modificărilor:**
   ```bash
   git add .
   git commit -m "Mesaj descriptiv al modificărilor"
   ```
3. **Împingerea ramurii către repository-ul central:**
   ```bash
   git push origin nume-ramura
   ```
4. **Crearea unui Pull Request (PR) pentru revizuire și integrare în ramura principală (main/master).**
5. **Revizuirea și aprobarea PR-ului de către ceilalți membri ai echipei.**
6. **După aprobarea PR-ului, integrarea modificărilor în ramura principală:**
    ```bash
    git checkout main
    git pull origin main
    git merge nume-ramura
    git push origin main
    ```

## To-Do

### Membru Echipă 1 (Sisteme de Bază, Gestionare Utilizatori & Moderare)

- [X] Implementează arhitectura pe straturi și maparea entităților în PostgreSQL.
- [X] Implementează CRUD pentru Utilizatori și rutele protejate (Routing guards).
- [X] Configurează autentificarea și stocarea parolelor în formă criptată.
- [X] Implementează privilegiile de Moderator: ștergerea sau editarea oricărui bug sau comentariu de pe site.
- [X] Implementează blocarea (ban) utilizatorilor pe perioadă nedeterminată și deblocarea acestora.
- [X] Restricționează accesul utilizatorilor blocați (mesaj la login, blocare acces prin URL).
- [ ] **Microserviciu:** Dezvoltă un serviciu independent care trimite e-mail și SMS la momentul blocării unui
  utilizator.

### Membru Echipă 2 (Motorul de Raportare Bug-uri & Fluxul de Lucru)

- [ ] Implementează CRUD pentru Bug-uri (autor, titlu, text, dată/oră creare, imagine, status).
- [ ] Implementează sistemul de etichete (tag-uri); permite utilizatorului să creeze o etichetă nouă dacă nu există una
  adecvată.
- [ ] Construiește lista de bug-uri, sortată descrescător după data creării (cele mai recente primele).
- [ ] Restricționează editarea/ștergerea bug-urilor doar pentru autorul lor inițial.
- [ ] Implementează filtrele pentru bug-uri: după etichetă, căutare text (în titlu), după utilizator, sau doar bug-urile
  proprii.
- [ ] Implementează statusul bug-ului: "Primit" (la postare), "În proces" (la primul comentariu), "Rezolvat" (când
  creatorul acceptă comentariile, blocând adăugarea altora noi).

### Membru Echipă 3 (Interacțiuni Sociale, Votare & Scoruri) - Afrasinei Serban

- [x] Implementează CRUD pentru Comentarii (autor, text, imagine, dată/oră creare). (TEST THIS)
- [x] Afișează lista de comentarii asociată fiecărui bug individual. (TEST THIS)
- [x] Restricționează editarea/ștergerea comentariilor doar pentru autorul lor inițial. (TEST THIS)
- [x] Implementează sistemul de votare (upvote/downvote, like/dislike) o singură dată pe bug/comentariu. (TEST THIS)
- [x] Blochează posibilitatea ca utilizatorii să își voteze propriile bug-uri sau comentarii. (TEST THIS)
- [x] Calculează și afișează numărul de voturi (poate fi negativ), sortând comentariile descrescător după acest număr. (TEST THIS)
- [ ] **Funcție Bonus 1:** Implementează algoritmul de scor (start la 0 pct; +2.5 pt bug votat pozitiv, +5 pt comentariu
  votat pozitiv; -1.5 pt bug votat negativ, -2.5 pt comentariu votat negativ, -1.5 dacă votează negativ un alt
  utilizator).
- [ ] Afișează scorul calculat lângă numele autorului (poate fi negativ).