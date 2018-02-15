/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.agnux.kemikal.controllers;

/**
 *
 * @author j2eeserver
 */
public class PotCatCusorder {

    public String cmd;
    public String usuario_id;
    public String salesman_id;
    public String customer_id;
    public String cust_df_id;
    public String warehouse_id;
    public String currency_id;
    public String sup_credays_id;
    public String forma_pago_id;
    public String met_pago_id;
    public String uso_id;
    public String pedido_id;
    public String tasaretimmex;
    public String currency_val;
    public String perc_desc;
    public String allow_desc;
    public String send_comments;
    public String flete_enable;
    public String send_route;
    public String comments;
    public String razon_desc;
    public String trans;
    public String date_lim;
    public String delivery_place;
    public String purch_order;
    public String account;
    public String no_cot;
    public String attrib_00;
    public String enable_00;
    public String attrib_01;
    public String enable_01;
    public String attrib_02;
    public String enable_02;
    public String matrix;
    public String importe_00;
    public String importe_01;
    public String importe_02;

    public String conform_cat_store() {
        return "select poc_cat_cusorder from poc_cat_cusorder("
                + "'" + this.cmd + "'::character varying,"
                + this.usuario_id + "::integer,"
                + this.salesman_id + "::integer,"
                + this.customer_id + "::integer,"
                + cust_df_id + "::integer,"
                + warehouse_id + "::integer,"
                + currency_id + "::integer,"
                + sup_credays_id + "::integer,"
                + forma_pago_id + "::integer,"
                + met_pago_id + "::integer,"
                + uso_id + "::integer,"
                + pedido_id + "::integer,"
                + tasaretimmex + "::double precision,"
                + currency_val + "::double precision,"
                + perc_desc + "::double precision,"
                + allow_desc + "::boolean,"
                + send_comments + "::boolean,"
                + flete_enable + "::boolean,"
                + send_route + "::boolean,"
                + "'" + comments + "'::text,"
                + "'" + razon_desc + "'::text,"
                + "'" + trans + "'::text,"
                + "'" + date_lim + "'::character varying,"
                + "'" + delivery_place + "'::character varying,"
                + "'" + purch_order + "'::character varying,"
                + "'" + account + "'::character varying,"
                + "'" + this.no_cot + "'::character varying,"
                + attrib_00 + "::integer,"
                + enable_00 + "::boolean,"
                + attrib_01 + "::integer,"
                + enable_01 + "::boolean,"
                + attrib_02 + "::integer,"
                + enable_02 + "::boolean,"
                + importe_00 + "::double precision,"
                + importe_01 + "::double precision,"
                + importe_02 + "::double precision,"
                + " array[" + matrix + "]::text[]);"; 
    }
}
