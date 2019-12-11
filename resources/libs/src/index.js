/*
 * react
 */
import React from 'react';
import ReactDOM from 'react-dom';

/*
 * graphql
 */
import { ApolloProvider, useQuery, useMutation } from '@apollo/react-hooks';
import { InMemoryCache, defaultDataIdFromObject } from 'apollo-cache-inmemory';
import { ApolloClient } from 'apollo-client';
import { ApolloLink } from 'apollo-link';
import { setContext } from 'apollo-link-context';
import { HttpLink } from 'apollo-link-http';
import * as gql from "graphql-tag";
import { parse } from "graphql";

/*
 * infinite scroll
 */
// import { RecyclerListView, DataProvider, LayoutProvider } from 'recyclerlistview/web';
import FlatList from 'flatlist-react';

/*
 * react
 */
window.React = React;
window.ReactDOM = ReactDOM;

/*
 * graphql
 */
window.ApolloProvider = ApolloProvider;
window.useQuery = useQuery;
window.useMutation = useMutation;
window.InMemoryCache = InMemoryCache;
window.defaultDataIdFromObject = defaultDataIdFromObject;
window.ApolloClient = ApolloClient;
window.ApolloLink = ApolloLink;
window.setContext = setContext;
window.HttpLink = HttpLink;
window.gql = gql;
window.parseGraphql = parse;
// window.printGraphql = print;

/*
 * infinite scroll
 */
window.FlatList = FlatList;
// window.RecyclerListView = RecyclerListView;
// window.DataProvider = DataProvider;
// window.LayoutProvider = LayoutProvider;
