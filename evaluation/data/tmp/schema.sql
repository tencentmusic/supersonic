PRAGMA foreign_keys = ON;
CREATE TABLE `Agencies` (
`agency_id` INTEGER PRIMARY KEY,
`agency_details` VARCHAR(255) NOT NULL
);

INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (1, 'Heller-Muller');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (2, 'Bergstrom, Koss and Ebert');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (3, 'Douglas-Langworth');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (4, 'Fadel Group');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (5, 'White, Corwin and Rath');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (6, 'Beatty-Sipes');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (7, 'West, Grady and Durgan');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (8, 'Hickle-Durgan');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (9, 'Grant, Jaskolski and Brekke');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (10, 'Muller, Klein and Kunde');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (11, 'Bins-Strosin');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (12, 'Emard-Fisher');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (13, 'Upton, Hayes and Schumm');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (14, 'Renner LLC');
INSERT INTO Agencies (`agency_id`, `agency_details`) VALUES (15, 'Cartwright, Ullrich and Gulgowski');


CREATE TABLE `Staff` (
`staff_id` INTEGER PRIMARY KEY,
`agency_id` INTEGER NOT NULL,
`staff_details` VARCHAR(255) NOT NULL
);

INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (1, 6, 'Rubie');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (2, 7, 'Sheridan');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (3, 10, 'Annabell');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (4, 2, 'Kendra');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (5, 7, 'Amara');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (6, 15, 'Lolita');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (7, 2, 'Hailie');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (8, 14, 'Armando');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (9, 10, 'Elroy');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (10, 8, 'Parker');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (11, 11, 'Clarissa');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (12, 5, 'Joaquin');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (13, 14, 'Antone');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (14, 14, 'Marques');
INSERT INTO Staff (`staff_id`, `agency_id`, `staff_details`) VALUES (15, 15, 'Margaret');


CREATE TABLE `Clients` (
`client_id` INTEGER PRIMARY KEY,
`agency_id` INTEGER NOT NULL,
`sic_code` VARCHAR(10) NOT NULL,
`client_details` VARCHAR(255) NOT NULL,
FOREIGN KEY (`agency_id` ) REFERENCES `Agencies`(`agency_id` )
);INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (1, 8, 'Mutual', 'Alta');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (2, 5, 'Bad', 'Mac');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (3, 3, 'Bad', 'Johnpaul');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (4, 5, 'Bad', 'Taurean');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (5, 14, 'Bad', 'Lucie');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (6, 8, 'Mutual', 'Rosa');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (7, 9, 'Mutual', 'Kirsten');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (8, 1, 'Mutual', 'Vincent');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (9, 9, 'Mutual', 'Heber');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (10, 9, 'Mutual', 'Callie');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (11, 14, 'Bad', 'Vaughn');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (12, 7, 'Mutual', 'Rae');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (13, 9, 'Mutual', 'Eloise');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (14, 11, 'Bad', 'Philip');
INSERT INTO Clients (`client_id`, `agency_id`, `sic_code`, `client_details`) VALUES (15, 1, 'Bad', 'Maximo');



CREATE TABLE `Invoices` (
`invoice_id` INTEGER PRIMARY KEY,
`client_id` INTEGER NOT NULL,
`invoice_status` VARCHAR(10) NOT NULL,
`invoice_details` VARCHAR(255) NOT NULL,
FOREIGN KEY (`client_id` ) REFERENCES `Clients`(`client_id` )
);
CREATE TABLE `Meetings` (
`meeting_id` INTEGER PRIMARY KEY,
`client_id` INTEGER NOT NULL,
`meeting_outcome` VARCHAR(10) NOT NULL,
`meeting_type` VARCHAR(10) NOT NULL,
`billable_yn` VARCHAR(1),
`start_date_time` DATETIME,
`end_date_time` DATETIME,
`purpose_of_meeting` VARCHAR(255),
`other_details` VARCHAR(255) NOT NULL,
FOREIGN KEY (`client_id` ) REFERENCES `Clients`(`client_id` )
);
CREATE TABLE `Payments` (
`payment_id` INTEGER NOT NULL ,
`invoice_id` INTEGER NOT NULL,
`payment_details` VARCHAR(255) NOT NULL,
FOREIGN KEY (`invoice_id` ) REFERENCES `Invoices`(`invoice_id` )
);

CREATE TABLE `Staff_in_Meetings` (
`meeting_id` INTEGER NOT NULL,
`staff_id` INTEGER NOT NULL,
FOREIGN KEY (`meeting_id` ) REFERENCES `Meetings`(`meeting_id` ),
FOREIGN KEY (`staff_id` ) REFERENCES `Staff`(`staff_id` )
);
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (1, 5, 'Working', 'excellent');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (2, 9, 'Starting', 'good');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (3, 15, 'Starting', 'excellent');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (4, 8, 'Starting', 'ok');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (5, 7, 'Finish', 'excellent');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (6, 8, 'Working', 'excellent');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (7, 7, 'Finish', 'excellent');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (8, 14, 'Finish', 'excellent');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (9, 12, 'Starting', 'good');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (10, 2, 'Finish', 'excellent');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (11, 11, 'Working', 'excellent');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (12, 9, 'Starting', 'good');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (13, 4, 'Starting', 'excellent');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (14, 14, 'Working', 'excellent');
INSERT INTO Invoices (`invoice_id`, `client_id`, `invoice_status`, `invoice_details`) VALUES (15, 14, 'Working', 'excellent');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (1, 15, 'Report', 'Team', '0', '2018-03-06 05:07:33', '2018-03-21 09:26:41', 'get proposal done', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (2, 3, 'Summary', 'Group', '0', '2018-03-16 02:24:10', '2018-03-21 17:57:59', 'vote for solutions', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (3, 4, 'Summary', 'Team', '1', '2018-03-06 21:02:06', '2018-03-01 05:10:01', 'get proposal done', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (4, 5, 'Vote results', 'Company', '1', '2018-03-10 05:03:46', '2018-02-25 23:20:12', 'weekly report', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (5, 14, 'Summary', 'Company', '1', '2018-02-26 18:35:24', '2018-03-12 18:48:55', 'weekly report', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (6, 8, 'Vote results', 'Team', '0', '2018-03-20 08:56:47', '2018-02-24 22:36:35', 'weekly report', 'absent staff');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (7, 13, 'Vote results', 'Group', '1', '2018-03-01 22:20:09', '2018-03-21 04:24:57', 'vote for solutions', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (8, 8, 'Report', 'Company', '0', '2018-03-16 06:38:16', '2018-03-20 12:24:04', 'weekly report', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (9, 4, 'Report', 'Group', '1', '2018-03-18 15:35:54', '2018-03-05 13:33:21', 'weekly report', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (10, 7, 'Summary', 'Team', '0', '2018-02-27 07:43:53', '2018-02-27 02:40:21', 'vote for solutions', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (11, 6, 'Summary', 'Team', '1', '2018-03-21 19:18:39', '2018-03-17 15:38:01', 'get proposal done', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (12, 10, 'Summary', 'Company', '0', '2018-03-17 09:56:49', '2018-03-03 21:51:07', 'monthly report', 'absent staff');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (13, 2, 'Report', 'Team', '1', '2018-02-28 15:39:03', '2018-03-17 13:09:45', 'weekly report', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (14, 2, 'Vote results', 'Group', '1', '2018-03-02 19:04:27', '2018-03-15 04:21:40', 'weekly report', '0');
INSERT INTO Meetings (`meeting_id`, `client_id`, `meeting_outcome`, `meeting_type`, `billable_yn`, `start_date_time`, `end_date_time`, `purpose_of_meeting`, `other_details`) VALUES (15, 2, 'Vote results', 'Company', '0', '2018-02-25 07:06:48', '2018-02-25 09:39:29', 'weekly report', '');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (1, 3, 'MasterCard');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (2, 5, 'Visa');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (3, 8, 'Discover Card');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (4, 11, 'MasterCard');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (5, 12, 'Visa');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (6, 9, 'Visa');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (7, 14, 'Visa');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (8, 2, 'American Express');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (9, 8, 'Visa');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (10, 8, 'Visa');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (11, 12, 'Visa');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (12, 11, 'Visa');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (13, 2, 'MasterCard');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (14, 6, 'Visa');
INSERT INTO Payments (`payment_id`, `invoice_id`, `payment_details`) VALUES (15, 3, 'Visa');
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (6, 7);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (14, 3);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (4, 5);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (1, 11);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (2, 10);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (9, 1);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (8, 3);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (8, 7);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (8, 6);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (3, 5);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (11, 2);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (10, 12);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (2, 8);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (6, 4);
INSERT INTO Staff_in_Meetings (`meeting_id`, `staff_id`) VALUES (14, 3);
