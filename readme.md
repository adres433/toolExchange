# toolExchange

### Table of content

- [Description EN](#description-pl)
- [Technologies](#technologies)
- [How start](#how-start)
- [Requirements](#requirements)
- [Description PL](#description-pl)
- [Used libraries](#used-libraries)

### DESCRIPTION EN

App writed for Yamazaki Mazak CEPL.

App for exchange service tools between service enginners on whole country.
Service enginners have some tools in equip, tools which are in personal and special tools which are only two or one pices for the whole country and they have to send these between self.
Application have to follow tools localizations and simpling them flow beetwen enginners.

In app:
- any statusstatuses of tools - defined in sharepoint list
- any quantity of tools in registry - defined in sharepoint list
- localization specified by current user - email and phone
- predefinied sms and email messages with information about necessary tools and contact request.
- graphical and text notice about technical reviev termin
- information about next user when tools is logistic status
- tools list show in simple and include the most necesary data

Major function this app is QR code scanner.
After made list of sharepoint with tools we have QR code for each position of list. These codes we can sticky on tools and scan when we got  and when we sending to next user.

App have a user manual in pdf file with many pics - invite for see [here](./manual.pdf)


### TECHNOLOGIES

- KOTLIN

### REQUIREMENTS

- ANDROID OS DEVICES
- OFFICE 365 - COMPANY ACCOUNT
- APP ONLY FOR YAMAZAKI MAZAK COMPANY
- RELEVANT LIST IN SHAREPOINT

### HOW START

- Install app in device with Android OS
- Grant premissions - phone and camera
- Start app
- Login to Office 365

### DESCRIPTION PL

Aplikacja napisana dla Yamazaki Mazak CEPL.

Aplikacja do wymiany narzędzi serwisowych pomiędzy serwisantami na terenie całego kraju.
Inżynierowie serwisowi mają na wyposażeniu jakieś narzędzia, narzędzia, które są na wyposażeniu osobistym oraz narzędzia specjalne, które stanowią tylko dwie lub jedną sztukę na cały kraj i muszą je przesyłać między sobą.
Aplikacja musi podążać za lokalizacją narzędzi i upraszczać ich przepływ między inżynierami.

w aplikacji:
- dowolne statusy narzędzi - zdefiniowane w liście sharepoint
- dowolna ilość narzędzi w rejestrze - zdefiniowana w liście sharepoint
- lokalizacja określona przez aktualnego użytkownika - e-mail i telefon
- predefiniowane wiadomości sms i e-mail z informacją o niezbędnych narzędziach i prośbą o kontakt.
- graficzne i tekstowe powiadomienie o terminie przeglądu technicznego
- informacja o następnym użytkowniku, gdy narzędzia mają status logistyczny
- lista narzędzi wyświetlana jest w prosty sposób i zawiera najpotrzebniejsze dane

Główną funkcją tej aplikacji jest skaner kodów QR.
Po sporządzeniu listy sharepointów wraz z narzędziami mamy kod QR dla każdej pozycji listy. Kody te możemy przyklejać na narzędziach i skanować, kiedy je otrzymamy i kiedy wyślemy je do następnego użytkownika.

Aplikacja posiada instrukcję obsługi w pliku pdf z wieloma zdjęciami - zapraszamy do obejrzenia [tutaj](./manual.pdf)

### USED LIBRARIES

##Libraries are using with MIT license or Apache 2.0 license:


With licenses can see here: [MIT](https://opensource.org/licenses/MIT) and [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)

1. ktor-client-core:1.6.4 - Apache 2.0
2. ktor-client-cio:1.6.4 - Apache 2.0
3. core-ktx:1.6.0 - Apache 2.0
4. appcompat:1.3.1 - Apache 2.0
5. material:1.4.0 - Apache 2.0
6. constraintlayout:2.1.1 - Apache 2.0
7. annotation:1.2.0 - Apache 2.0
8. lifecycle-livedata-ktx:2.3.1 - Apache 2.0
9. lifecycle-viewmodel-ktx:2.3.1 - Apache 2.0
10. bcrypt:0.9.0 - Apache 2.0
11. cardview:1.0.0 - Apache 2.0
12. recyclerview:1.2.1 - Apache 2.0
13. mediarouter:1.2.5 - Apache 2.0
14. msal:2.2.1 - MIT Copyright (c) Microsoft Corporation. All rights reserved.
15. jackson-module-kotlin:2.13.0 - Apache 2.0
16. code-scanner:2.1.0 - MIT Copyright (c) 2017 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
17. navigation-fragment-ktx:2.3.5 - Apache 2.0
18. navigation-ui-ktx:2.3.5 - Apache 2.0