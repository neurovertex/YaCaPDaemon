Note:
(en) I like to keep text in my projects in English as much as possible, however, given the special status of this one, a French version of the description is available below.
(fr) J'aime garder en anglais autant de texte que possible dans mes projets, cepandant, étant donné la spécificité géographique de celui-ci, une version française est disponible en deuxième partie du document.

YaCaP Daemon
============

Introduction
------------

YaCaP is a captive portal developed by the [CIRIL](http://reseau.ciril.fr/) for users of the Lothaire network, which serves higher education and research institutions in Lorraine.
I've been living in university residences for three years and have experienced issues with this portal, ranging from being disconnected unexpectedly to server or network errors, I decided to write YaCaPDaemon to take care of keeping me logged in.

Preliminary warning
-------------------
If you don't live in a university residence in Lorraine, France, this program won't be any useful to you as is. You can still read the code and try to understand/copy the way it works of course.
The main reason I'm posting this project on GitHub is to be able to share it with classmates and local friends who might be interested.
Also, for now the program only supports login via University of Lorraine and the crous portal, it won't work if you're not using the CrousNet wifi. I might add support for the university wifi, but wherever it's available eduroam is too, which is way more convenient to use.

Building and Running the project
--------------------------------

You need [JSoup](http://jsoup.org/) both for building and running the project.
Assuming you're at the root of the project and have jsoup.jar in a lib folder, you can build the project to a bin folder with :

	javac -cp lib/jsoup.jar -d bin src/eu/neurovertex/yacapd/*.java src/eu/neurovertex/yacapd/gui/*.java

and you can run the program with :

	javac -cp "lib/jsoup.jar;bin" eu.neurovertex.yacapd.Main

You can make a script or a .jar for it but for now the only way the program knows to ask your login/password is via standard input/output so you'll need to launch it through a terminal or IDE at least the fist time.

Features and Usage
------------------

The daemon has a minimal GUI (a systray icon) that should show you its current state and allow you to send it various commands. The state is described by a solid colour circle, if everything goes well you should see green which means connected. If you see red, the application got a "bad login" status from the server and you have to re-enter your login/password in the console. Yellow means the application is paused. Other colours are temporary states and you shouldn't see them much unless you have pretty bad network lag. In the tray icon's popup menu, the reconnect option will log you out then back in, whereas logout will log you out then pause the application until you use reconnect.

YaCaP Daemon (fr)
=================

Introduction
------------

YaCaP est un portail captif développé par le [CIRIL](http://reseau.ciril.fr/) pour les utilisateurs du réseau Lothaire, qui dessert les établissement d'enseignement suppérieure et de recherche en Lorraine.
J'ai vécu trois ans en Cité Universitaire, et ai eu de multiples problèmes avec YaCaP, j'ai donc décidé d'écrire un programme simple pour maintenir la connexion ouverte.

Avertissement préalable
-----------------------

Si vous n'habitez pas en Lorraine, ce programme ne vous sera d'aucune utilité tel quel. Vous pouvez toujours lire le code et copier son fonctionnement bien sûr.
La raison principale pour laquelle je publie ce projet sur GitHub est de pouvoir le partager avec des collègues étudiants et amis nancéens qui pourraient en avoir besoin.
De plus, le programme ne supporte pour l'instant que des logins de l'Université de Lorraine sur le portail du CROUS, ça ne marchera pas si vous n'êtes pas sur CrousNet. Je pourrai ajouter d'autres méthodes/réseaux supportés plus tard mais là où il y a le wifi de l'université, il y a généralement eduroam qui est infiniment plus facile et pratique à utiliser.

Compiler et Lancer le Projet
----------------------------

Vous aurez besoin de [JSoup](http://jsoup.org/) pour compiler ou lancer le programme.
Si vous êtez à la racine du projet et avez jsoup.rar dans un dossier lib, vous pouvez compiler le projet vers un dossier bin avec :

	javac -cp lib/jsoup.jar -d bin src/eu/neurovertex/yacapd/*.java src/eu/neurovertex/yacapd/gui/*.java

Et lancer le programmer avec :

	javac -cp "lib/jsoup.jar;bin" eu.neurovertex.yacapd.Main

Vous pouvez faire un script ou un .jar pour ça mais le seul moyen que le programme connait pour demander vos identifiants est l'entrée/sortie standard donc vous devrez le lancer à partir d'un terminal ou d'un IDE au moins la première fois.

Fonctionalités et Utilisation
-----------------------------

Le daemon a une interface graphique minimale (une icone dans la barre des tâches) affichant l'état actuel et permettant un control minimal de l'application. L'état est décrit par un cercle de couleur unie, qui devrait être vert pour "connecté" si tout va bien. Si le cercle est rouge, le serveur a renvoyé une erreur d'identifiants et vous devez les entrer à nouveau par le terminal. Jaune signifie que l'application est en pause, et les autres couleurs ne sont que des états temporaires que vous ne devriez pas voir à moins d'avoir une latence réseau importante. Dans le menu contextuel de l'icone, "reconnect" et "logout" vous déconnecteront, mais reconnect relancera la connexion immédiatement alors que  logout mettra l'application en pause (état jaune) jusqu'à ce que vous faisiez reconnect.


